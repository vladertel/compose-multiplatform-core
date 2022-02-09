/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.resolve.BindingTrace

/*
* JS doesn't have ability to extend kotlin's FunctionN types.
* Therefore composable lambda invocations need to be altered.
*
* Given composableContent: @Composable () -> Unit,
* instead of calling it like a function `composableContent(composer, 1)`,
* we make it call `invoke` method of ComposableLambda: `composableContent.invoke(composer, 1)`
*
* Note:
* `composableContent: @Composable () -> Unit` is not always of ComposableLambda type.
* In such cases, call remains unchanged: `composableContent(composer, 1)`
*/
class FixComposableLambdaCalls(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    metrics: ModuleMetrics
) : AbstractComposeLowering(
    context, symbolRemapper, bindingTrace, metrics
) {

    override fun lower(module: IrModuleFragment) {
        require(context.platform.isJs()) {
            "FixComposableLambdaCalls transformation is intended only for kotlin/js targets"
        }
        module.transformChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        if (declaration.name.contains("RecomposeScopeImpl.kt")) {
            return declaration
        }
        return super.visitFile(declaration)
    }

    private fun IrType.hasComposerDirectly(): Boolean {
        if (this == composerType) return true

        return when (this) {
            is IrSimpleType -> arguments.any { (it as? IrType) == composerType }
            else -> false
        }
    }

    private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

    private val composableLambdaClassImpl = symbolRemapper.getReferencedClass(
        context.referenceClass(
            FqName("androidx.compose.runtime.internal.ComposableLambdaImpl")
        )!!
    ).owner

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration == composableLambdaClassImpl) return declaration
        return super.visitClass(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val original = super.visitCall(expression) as IrCall

        if (!original.symbol.owner.isOperator) return original
        if (original.symbol.owner.name.asString() != "invoke") return original

        val dispatchReceiver = original.dispatchReceiver ?: return original

        if (!dispatchReceiver.type.isFunction() || !dispatchReceiver.type.hasComposerDirectly()) {
            return original
        }

        // skip if lambda has non-Unit return type
        if ((dispatchReceiver.type as IrSimpleType).arguments.last().typeOrNull?.isUnit() != true) {
            return original
        }

        val valueParameter = (dispatchReceiver as? IrGetValue)?.symbol?.owner as? IrValueParameter

        // if dispatchReceiver is a value parameter, we want to transform only calls which won't
        // be inlined, so we keep inline lambdas as they are
        if (valueParameter != null
            && (valueParameter.parent as? IrSimpleFunction)?.isInline == true
            && !valueParameter.isNoinline
        ) {
            return original
        }

        val targetInvoke = composableLambdaClassImpl.declarations.firstOrNull {
            it is IrFunction
                && it.name.asString() == "invoke"
                && it.valueParameters.size == original.valueArgumentsCount
        } as? IrSimpleFunction ?: error(
            "ComposableLambdaImpl.invoke() not found " + original.dump()
        )

        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = original.type,
            symbol = targetInvoke.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = original.valueArgumentsCount,
            origin = null
        ).also {
            it.dispatchReceiver = dispatchReceiver
            repeat(original.valueArgumentsCount) { ix ->
                it.putValueArgument(ix, original.getValueArgument(ix))
            }
        }
    }
}