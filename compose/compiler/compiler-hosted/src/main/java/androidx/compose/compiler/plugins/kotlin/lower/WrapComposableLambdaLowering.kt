/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.BindingTrace

/**
 * This lowering is necessary for k/js, although it works for jvm and k/native too.
 *
 * Finds all calls to `composableLambda(...)` and `composableLambdaInstance(...)`, then
 * changes them to corresponding `wrappedComposableLambda` and `wrappedComposableLambdaInstance`.
 *
 * Context:
 *
 * 1) `composableLambda` and `composableLambdaInstance` return an instance of ComposableLambda which
 * doesn't implement FunctionX interfaces in k/js (due to k/js limitation), therefore they can't be
 * invoked using Function.invoke symbol.
 *
 * 2) The initial workaround (in [ComposerLambdaMemoization.wrapFunctionExpression]) was wrapping a
 * ComposableLambda invocation into a function ref (so it's possible to use `Function.invoke`).
 * The cons of that workaround is that it led to new lambda instantiations on every invocation +
 * prevented the composable body skipping logic (some runtime tests were failing).
 *
 * 3) The alternative: Look for all ComposableLambda invoke calls and transform them to
 * `ComposableLambda.invoke` symbol instead of `Function.invoke`. Such a transformation
 * was implemented in JB fork, but it's much less straightforward.
 *
 * 4) This lowering: It wraps ComposableLambda invocation into a corresponding lambda,
 * instance of which is "remembered" along with ComposableLambda instance.
 * (It's similar to wrapping into a function ref, but lambda instance gets created only once).
 *
 * Wrappers (defined in runtime):
 *
 * `wrappedComposableLambda` and `wrappedComposableLambdaInstance` return a lambda of
 * correct arity (same arity as ComposableLambda.block). Invoking such a lambda will invoke
 * the wrapped ComposableLambda.
 *
 * Example of wrappedComposableLambda:
 * w/o WrapComposableLambdaLowering:
 * ```
 * ComposableA(composableLambda(..., { ComposableB() }))
 * ```
 * w/ WrapComposableLambdaLowering:
 * ```
 * ComposableA(wrappedComposableLambda(..., { ComposableB() }))
 * ```
 *
 * Example of wrappedComposableLambdaInstance:
 * w/o WrapComposableLambdaLowering:
 * ```
 * object ComposableSingletons%AbcKt {
 *    val lambda-1: Function2<Composer, Int, Unit> =
 *        composableLambdaInstance(..., { ComposableB() })
 * }
 * ```
 * w/ WrapComposableLambdaLowering:
 * object ComposableSingletons%AbcKt {
 *    val lambda-1: Function2<Composer, Int, Unit> = wrappedComposableLambdaInstance(
 *        2,  // arity
 *        composableLambdaInstance(..., { ComposableB() })
 *    )
 * }
 */
class WrapComposableLambdaLowering(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    metrics: ModuleMetrics
) : AbstractComposeLowering(context, symbolRemapper, bindingTrace, metrics) {

    companion object {
        // To be wrapped
        const val COMPOSABLE_LAMBDA = "composableLambda"
        const val COMPOSABLE_LAMBDA_INSTANCE = "composableLambdaInstance"

        // The wrappers
        const val WRAPPED_COMPOSABLE_LAMBDA = "wrappedComposableLambda"
        const val WRAPPED_COMPOSABLE_LAMBDA_INSTANCE = "wrappedComposableLambdaInstance"
    }

    private val composableLambdaSymbol = symbolRemapper.getReferencedSimpleFunction(
        getTopLevelFunctions(ComposeFqNames.internalFqNameFor(COMPOSABLE_LAMBDA)).first()
    )
    private val composableLambdaInstanceSymbol = symbolRemapper.getReferencedSimpleFunction(
        getTopLevelFunctions(ComposeFqNames.internalFqNameFor(COMPOSABLE_LAMBDA_INSTANCE)).first()
    )
    private val wrappedComposableLambdaSymbol = symbolRemapper.getReferencedSimpleFunction(
        getTopLevelFunctions(ComposeFqNames.internalFqNameFor(WRAPPED_COMPOSABLE_LAMBDA)).first()
    )
    private val wrappedComposableLambdaInstanceSymbol = symbolRemapper.getReferencedSimpleFunction(
        getTopLevelFunctions(ComposeFqNames.internalFqNameFor(WRAPPED_COMPOSABLE_LAMBDA_INSTANCE))
            .first()
    )

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val original = super.visitCall(expression) as IrCall
        return when (expression.symbol) {
            composableLambdaSymbol -> {
                visitComposableLambdaCall(original)
            }
            composableLambdaInstanceSymbol -> {
                visitComposableLambdaInstanceCall(original)
            }
            else -> original
        }
    }

    private fun visitComposableLambdaCall(originalCall: IrCall): IrCall {
        val lambda = originalCall.getValueArgument(originalCall.valueArgumentsCount - 1)
            as IrFunctionExpression

        return IrCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = lambda.type,
            symbol = wrappedComposableLambdaSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 5,
            origin = originalCall.origin
        ).apply {
            putValueArgument(0, originalCall.getValueArgument(0)) // composer
            putValueArgument(1, originalCall.getValueArgument(1)) // key
            putValueArgument(2, originalCall.getValueArgument(2)) // tracked
            val invokeArgumentsCount = IrConstImpl.int(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                context.irBuiltIns.intType,
                lambda.function.valueParameters.size +
                    if (lambda.function.extensionReceiverParameter != null) 1 else 0
            )
            putValueArgument(3, invokeArgumentsCount) // invokeArgumentsCount
            putValueArgument(4, lambda)
        }
    }

    private fun visitComposableLambdaInstanceCall(originalCall: IrCall): IrCall {
        val lambda = originalCall.getValueArgument(originalCall.valueArgumentsCount - 1)
            as IrFunctionExpression
        return IrCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = lambda.type,
            symbol = wrappedComposableLambdaInstanceSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 2,
            origin = originalCall.origin
        ).apply {
            val invokeArgumentsCount = IrConstImpl.int(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                context.irBuiltIns.intType,
                lambda.function.valueParameters.size +
                    if (lambda.function.extensionReceiverParameter != null) 1 else 0
            )
            putValueArgument(0, invokeArgumentsCount) // invokeArgumentsCount
            putValueArgument(1, originalCall) // call to composableLambda
        }
    }
}