/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.ClassStabilityTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunInterfaceLowering
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunctionBodyTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposableSymbolRemapper
import androidx.compose.compiler.plugins.kotlin.lower.ComposerIntrinsicTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposerLambdaMemoization
import androidx.compose.compiler.plugins.kotlin.lower.ComposerParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import androidx.compose.compiler.plugins.kotlin.lower.KlibAssignableParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.LiveLiteralTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.CreateDecoysTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.RecordDecoySignaturesTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.SubstituteDecoyCallsTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoyImplementation
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class ComposeIrGenerationExtension(
    @Suppress("unused") private val liveLiteralsEnabled: Boolean = false,
    @Suppress("unused") private val liveLiteralsV2Enabled: Boolean = false,
    private val sourceInformationEnabled: Boolean = true,
    private val intrinsicRememberEnabled: Boolean = true,
    private val decoysEnabled: Boolean = false,
) : IrGenerationExtension {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val isKlibTarget = !pluginContext.platform.isJvm()
        VersionChecker(pluginContext).check()

        // TODO: refactor transformers to work with just BackendContext
        val bindingTrace = DelegatingBindingTrace(
            pluginContext.bindingContext,
            "trace in " +
                "ComposeIrGenerationExtension"
        )

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = ComposableSymbolRemapper()

        ClassStabilityTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        LiveLiteralTransformer(
            liveLiteralsEnabled || liveLiteralsV2Enabled,
            liveLiteralsV2Enabled,
            DurableKeyVisitor(),
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        ComposableFunInterfaceLowering(pluginContext).lower(moduleFragment)

        // Memoize normal lambdas and wrap composable lambdas
        ComposerLambdaMemoization(pluginContext, symbolRemapper, bindingTrace).lower(moduleFragment)

        val idSignatureBuilder = when {
            pluginContext.platform.isJs() -> IdSignatureSerializer(JsManglerIr).also {
                it.table = DeclarationTable(JsGlobalDeclarationTable(it, pluginContext.irBuiltIns))
            }
            else -> null
        }
        if (decoysEnabled) {
            require(idSignatureBuilder != null) {
                "decoys are not supported for ${pluginContext.platform}"
            }

            CreateDecoysTransformer(pluginContext, symbolRemapper, bindingTrace, idSignatureBuilder)
                .lower(moduleFragment)
            SubstituteDecoyCallsTransformer(
                pluginContext,
                symbolRemapper,
                bindingTrace,
                idSignatureBuilder
            ).lower(moduleFragment)
        }

        // transform all composable functions to have an extra synthetic composer
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        ComposerParamTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace,
            decoysEnabled
        ).lower(moduleFragment)

        // transform calls to the currentComposer to just use the local parameter from the
        // previous transform
        ComposerIntrinsicTransformer(pluginContext, decoysEnabled).lower(moduleFragment)

        ComposableFunctionBodyTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace,
            sourceInformationEnabled,
            intrinsicRememberEnabled
        ).lower(moduleFragment)

        fun IrType.replaceArgumentsWithStarProjections(): IrType =
            when (this) {
                is IrSimpleType -> IrSimpleTypeImpl(
                    classifier,
                    hasQuestionMark,
                    List(arguments.size) { IrStarProjectionImpl },
                    annotations,
                    abbreviation
                )
                else -> this
            }

        val _composerIrClass = pluginContext.referenceClass(ComposeFqNames.Composer)?.owner!!
        val composerIrClass = symbolRemapper.getReferencedClass(_composerIrClass.symbol).owner
        val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()


        if (decoysEnabled) {
            moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
                private fun IrType.isFunction(): Boolean {
                    val classifier = classifierOrNull ?: return false
                    val name = classifier.descriptor.name.asString()
                    if (!name.startsWith("Function")) return false
                    return true
                }

                private fun IrType.hasComposer(): Boolean {
                    if (this == composerType) return true

                    return when (this) {
                        is IrSimpleType -> arguments.any { (it as? IrType)?.hasComposer() == true }
                        else -> false
                    }
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                    if (declaration.name.asString().contains
                            ("invokeComposableForResult\$composable")) {
                        return declaration
                    }
                    return super.visitSimpleFunction(declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val original = super.visitCall(expression) as IrCall

//                    if (original.origin != IrStatementOrigin.INVOKE) {
//                        return original
//                    }

                    if (!original.symbol.owner.isOperator) return original
                    if (original.symbol.owner.name.asString().contains("<get-entries>")) return original
                    if (original.symbol.owner.name.asString() != "invoke") return original
                    val dispatchReceiver = original.dispatchReceiver ?: return original

                    val valueParameter = (dispatchReceiver as? IrGetValue)
                        ?.symbol?.owner as? IrValueParameter //?: return original


                    if (valueParameter != null
                        && (valueParameter.parent as? IrSimpleFunction)?.isInline == true
                        && !valueParameter.isNoinline
                    ) {
                        return original
                    }


                    if (!dispatchReceiver.type.isFunction() || !dispatchReceiver.type.hasComposer()) {
                        return original
                    }

                    val sym = symbolRemapper.getReferencedClass(
                        pluginContext.referenceClass(
                            FqName("androidx.compose.runtime.internal.ComposableLambdaImpl")
                        )!!
                    ).owner

                    val targetInvoke = sym.declarations.firstOrNull {
                        it is IrFunction
                            && it.name.asString() == "invoke"
                            && it.valueParameters.size == original.valueArgumentsCount
                    } as? IrSimpleFunction ?: error(
                        "ComposableLambdaImpl.call() not found " +
                            original.dump()
                    )

                    return IrIfThenElseImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.unitType
                    ).apply {
                        branches.add(IrBranchImpl(
                            condition = IrTypeOperatorCallImpl(
                                startOffset = UNDEFINED_OFFSET,
                                endOffset = UNDEFINED_OFFSET,
                                type = pluginContext.irBuiltIns.booleanType,
                                operator = IrTypeOperator.INSTANCEOF,
                                typeOperand = sym.defaultType,
                                argument = dispatchReceiver
                            ),
                            result = IrCallImpl(
                                startOffset = original.startOffset,
                                endOffset = original.endOffset,
                                type = original.type,
                                symbol = targetInvoke.symbol,
                                typeArgumentsCount = 0,
                                valueArgumentsCount = original.valueArgumentsCount,
                                origin = null//original.origin
                            ).also {
                                it.dispatchReceiver = dispatchReceiver
                                repeat(original.valueArgumentsCount) { ix ->
                                    it.putValueArgument(ix, original.getValueArgument(ix))
                                }
                            }
                        ))
                        branches.add(
                            IrElseBranchImpl(
                                condition = IrConstImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    pluginContext.irBuiltIns.booleanType,
                                    IrConstKind.Boolean,
                                    true
                                ),
                                result = expression
                            )
                        )
                    }
                }
            })
        }


        if (decoysEnabled) {
            require(idSignatureBuilder != null) {
                "decoys are not supported for ${pluginContext.platform}"
            }

            RecordDecoySignaturesTransformer(
                pluginContext,
                symbolRemapper,
                bindingTrace,
                idSignatureBuilder
            ).lower(moduleFragment)
        }

        if (isKlibTarget) {
            KlibAssignableParamTransformer(
                pluginContext,
                symbolRemapper,
                bindingTrace
            ).lower(moduleFragment)
        }

    }
}
