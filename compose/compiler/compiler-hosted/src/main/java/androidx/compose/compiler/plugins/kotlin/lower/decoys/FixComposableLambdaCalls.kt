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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
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
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
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

    private fun IrType.hasComposer(): Boolean {
        if (this == composerType) return true

        return when (this) {
            is IrSimpleType -> arguments.any { (it as? IrType)?.hasComposer() == true }
            else -> false
        }
    }

    private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

    private val composableLambdaClassImpl = symbolRemapper.getReferencedClass(context.referenceClass(
        FqName("androidx.compose.runtime.internal.ComposableLambdaImpl")
    )!!).owner

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration == composableLambdaClassImpl) return declaration
        return super.visitClass(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val original = super.visitCall(expression) as IrCall

        if (!original.symbol.owner.isOperator) return original
        if (original.symbol.owner.name.asString() != "invoke") return original

        val dispatchReceiver = original.dispatchReceiver ?: return original

        if (!dispatchReceiver.type.isFunction() || !dispatchReceiver.type.hasComposer()) {
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

        // There are cases when dispatchReceiver is not of ComposableLambdaImpl type, so
        // we need to add a type check like below at runtime:
        // if (content is ComposableLambdaImpl)
        //     content.invoke(composer, 14 & changed)
        // else {
        //     content(composer, 14 & changed)
        // }
        return IrIfThenElseImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(
                // if (content is ComposableLambdaImpl)
                condition = IrTypeOperatorCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = context.irBuiltIns.booleanType,
                    operator = IrTypeOperator.INSTANCEOF,
                    typeOperand = composableLambdaClassImpl.defaultType,
                    argument = dispatchReceiver
                ),
                // then: content.invoke(composer, 14 & changed)
                result = IrCallImpl(
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
            ))
            branches.add(
                // else: content(composer, 14 & changed)
                IrElseBranchImpl(
                    condition = IrConstImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.booleanType,
                        IrConstKind.Boolean,
                        true
                    ),
                    result = expression
                )
            )
        }
    }
}