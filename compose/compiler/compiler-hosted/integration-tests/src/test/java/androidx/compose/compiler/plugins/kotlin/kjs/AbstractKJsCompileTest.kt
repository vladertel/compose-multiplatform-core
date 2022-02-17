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

package androidx.compose.compiler.plugins.kotlin.kjs

import androidx.compose.compiler.plugins.kotlin.ComposeComponentRegistrar
import androidx.compose.compiler.plugins.kotlin.ComposeConfiguration
import androidx.compose.compiler.plugins.kotlin.createFile
import androidx.compose.compiler.plugins.kotlin.kjs.AbstractKJsCompileTest.Companion.FAKE_FILE_NAME
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import java.io.File
import java.util.AbstractMap
import java.util.concurrent.Executors
import java.util.function.Function
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.backend.common.CheckDeclarationParentsVisitor
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.junit.Assert.assertTrue


abstract class AbstractKJsCompileTest {

    private val classpath = System.getProperty("androidx.compose.js.classpath").orEmpty()
        .split(":")
        .map { File(it) }
        .filter { isKotlinLibrary(it) }

    private val kotlinEnvironment = KotlinEnvironment(
        classpath = emptyList(),
        additionalJsClasspath = classpath
    ).apply {
        environment {
            it.configuration.put(ComposeConfiguration.DECOYS_ENABLED_KEY, true)
            ComposeComponentRegistrar.registerProjectExtensions(
                it.project as MockProject,
                it.configuration
            )
        }
    }

    private val kotlinToJSTranslator = KotlinToJSTranslator(kotlinEnvironment)

    private fun compileKtToJsExecutable(
        source: String,
        testFileIrReader: ((IrFile) -> Unit)? = null
    ): String {
        val project = Project(
            files = listOf(
                ProjectFile(text = source, name = FAKE_FILE_NAME)
            )
        )

        return kotlinEnvironment.environment { env ->
            val ktfiles = project.files.map {
                createFile(it.name, it.text, env.project)
            }
            kotlinToJSTranslator.doTranslateWithIr(
                files = ktfiles,
                arguments = project.args.split(" "),
                coreEnvironment = env,
                testFileIrReader = testFileIrReader, // use this to read ir of FAKE_FILE_NAME
            )
        }
    }

    protected fun kjsComposableTestShouldFail(block: KJsComposableTestCase.() -> Unit): Throwable {
        var throwable: Throwable? = null
        try {
            kjsComposableTest(block)
        } catch (t: Throwable) {
            throwable = t
        }
        assertTrue("Exception was expected", throwable != null)
        return throwable!!
    }

    class OnJsSourceApplier {
        private val patches = mutableListOf<String>()

        fun exportGetterFor(propName: String) {
            patches.add(" _.get_$propName = function() { return $propName };")
        }

        fun exportGetterFor(vararg propNames: String) {
            propNames.forEach {
                patches.add(" _.get_$it = function() { return $it };")
            }
        }

        fun export(anything: String) {
            patches.add(" _.$anything = $anything;")
        }

        fun apply(originalJsCode: String): String {
            val patch = patches.joinToString(separator = "\n")
            return originalJsCode.replace(
                oldValue = APPLY_PATCH_PLACEHOLDER,
                newValue = patch
            )
        }
    }

    protected fun kjsComposableTest(block: KJsComposableTestCase.() -> Unit) = runBlocking {
        val testCase = KJsComposableTestCase().apply(block)
        val jsCode = compileKtToJsExecutable(
            source = """
                $COMMON_IMPORTS
                $TEST_DEPENDENCIES_IMPL
                
                ${testCase.kotlinCodeBlock()}
            """.trimIndent(),
            testFileIrReader = testCase.onIrReadyCallback
        ).replace(
            oldValue = "return _;",
            newValue = " //APPLY_PATCH\n return _;"
        ) + TEST_HELPER_JS_CODE_TO_APPEND

        val jsCodeFinal = with(OnJsSourceApplier()) {
            testCase.applyOnJsCodeBlock(this)
            apply(jsCode)
        }

        testCase.onJsCodeReadyCallback?.invoke(jsCodeFinal)

        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val coroutineScope = CoroutineScope(dispatcher)

        val startMs = System.currentTimeMillis()
        val handledExceptions = mutableListOf<Throwable>()

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            handledExceptions.add(exception)
        }

        val j = coroutineScope.launch(coroutineExceptionHandler) {
            val graaljsEngine = ScriptEngineManager().getEngineByName("graal.js")

            val bindings = graaljsEngine.getBindings(ScriptContext.ENGINE_SCOPE)
            bindings.put("polyglot.js.allowAllAccess", true)
            bindings.put("currTime", java.util.function.IntSupplier {
                (System.currentTimeMillis() - startMs).toInt()
            })
            bindings.put("onRecompositionComplete", java.util.function.Supplier {
                testCase.waitForRecompositionCompleteContinuation?.resume(Unit)
                testCase.waitForRecompositionCompleteContinuation = null
                0
            })
            bindings.put(
                "setTimeout",
                java.util.function.BiFunction<Function<Array<Any>, Any>, Int, Int> { handler, t ->
                    launch {
                        delay(t.toLong())
                        handler.apply(emptyArray())
                    }
                    0
                })

            val module: AbstractMap<Any, Any> =
                graaljsEngine.eval(jsCodeFinal) as AbstractMap<Any, Any>

            testCase.verificationBlock(coroutineScope, module)
            dispatcher.close()
        }

        j.join()

        if (handledExceptions.isNotEmpty()) {
            throw handledExceptions.first()
        }
    }

    companion object {
        const val FAKE_FILE_NAME = "KJSComposableTest.kt"
        const val APPLY_PATCH_PLACEHOLDER = "//APPLY_PATCH"

        const val COMMON_IMPORTS = """
            import androidx.compose.runtime.*
            import kotlinx.coroutines.*
            import kotlinx.coroutines.channels.*
            import androidx.compose.runtime.snapshots.*
            import kotlin.coroutines.*
            import kotlin.js.*
        """

        const val TEST_DEPENDENCIES_IMPL = """
            class JsMicrotasksDispatcher : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    Promise.resolve(Unit).then { block.run() }
                }
            }
            
            private class UnitApplier : Applier<Unit> {
                override val current: Unit
                    get() = Unit
        
                override fun down(node: Unit) {}
                override fun up() {}
                override fun insertTopDown(index: Int, instance: Unit) {}
                override fun insertBottomUp(index: Int, instance: Unit) {}
                override fun remove(index: Int, count: Int) {}
                override fun move(from: Int, to: Int, count: Int) {}
                override fun clear() {}
            }
            
            private fun createRecomposer(): Recomposer {
                val mainScope = CoroutineScope(
                    NonCancellable + JsMicrotasksDispatcher() + SixtyFpsMonotonicFrameClock
                )
        
                return Recomposer(mainScope.coroutineContext).also {
                    mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        it.runRecomposeAndApplyChanges()
                    }
                }
            }
            
            external fun currTime(): Int // time ms
            external fun onRecompositionComplete() // a callback to hosting environment
            
            private object SixtyFpsMonotonicFrameClock : MonotonicFrameClock {
                private const val fps = 60
        
                override suspend fun <R> withFrameNanos(
                    onFrame: (Long) -> R
                ): R {
                    delay(1000L / fps)
                    // currTime implemented in bindings
                    val r = onFrame(currTime().toLong())
                    
                    onRecompositionComplete() // implemented in bindings
                    return r
                }
            }
            
            fun ensureStarted() {
                val channel = Channel<Unit>(Channel.CONFLATED)
                CoroutineScope(JsMicrotasksDispatcher()).launch {
                    channel.consumeEach {
                        Snapshot.sendApplyNotifications()
                    }
                }
                Snapshot.registerGlobalWriteObserver {
                    channel.trySend(Unit)
                }
            }
            
            fun createComposition() = Composition(UnitApplier(), createRecomposer())
            
            fun main() {
                ensureStarted() 
            }
        """

        const val TEST_HELPER_JS_CODE_TO_APPEND = "moduleId;"
    }
}


private class KotlinEnvironment(
    val classpath: List<File>,
    val additionalJsClasspath: List<File>
) {
    companion object {
        private val additionalCompilerArguments: List<String> = listOf()
    }

    internal val jsLibraries = additionalJsClasspath.map { it.absolutePath }

    fun <T> environment(f: (KotlinCoreEnvironment) -> T): T {
        return f(environment)
    }

    private val configuration = createConfiguration()

    internal val jsConfiguration: CompilerConfiguration = configuration.copy().apply {
        put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
        put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        put(JSConfigurationKeys.LIBRARIES, jsLibraries)
    }

    private val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    private val messageCollector =
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    val jsIrPhaseConfig =
        createPhaseConfig(jsPhases, K2JsIrCompiler().createArguments(), messageCollector)

    val jsIrResolvedLibraries = jsResolveLibraries(
        jsLibraries,
        emptyList(),
        logger
    )

    private val environment = KotlinCoreEnvironment.createForProduction(
        parentDisposable = Disposer.newDisposable(),
        configuration = configuration.copy(),
        configFiles = EnvironmentConfigFiles.JS_CONFIG_FILES
    )

    private fun createConfiguration(): CompilerConfiguration {
        val arguments = K2JVMCompilerArguments()
        parseCommandLineArguments(additionalCompilerArguments, arguments)
        return CompilerConfiguration().apply {
            addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
            val messageCollector = MessageCollector.NONE
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(CommonConfigurationKeys.MODULE_NAME, "web-module")
            put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)

            languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)

            // it uses languageVersionSettings that was set above
            configureAdvancedJvmOptions(arguments)
            put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
        }
    }
}

private data class Project(
    val args: String = "",
    val files: List<ProjectFile> = listOf(),
    val confType: ProjectType = ProjectType.JS_IR
)

private data class ProjectFile(val text: String = "", val name: String = "")

private enum class ProjectType(val id: String) {
    JAVA("java"),
    JUNIT("junit"),
    CANVAS("canvas"),
    JS("js"),
    JS_IR("js-ir");

    fun isJsRelated(): Boolean = this == JS || this == JS_IR || this == CANVAS
}


private class KotlinToJSTranslator(
    private val kotlinEnvironment: KotlinEnvironment,
) {
    fun doTranslateWithIr(
        files: List<KtFile>,
        arguments: List<String>,
        coreEnvironment: KotlinCoreEnvironment,
        testFileIrReader: ((IrFile) -> Unit)? = null
    ): String {
        val currentProject = coreEnvironment.project

        val sourceModule = prepareAnalyzedSourceModule(
            currentProject,
            files,
            kotlinEnvironment.jsConfiguration,
            kotlinEnvironment.jsLibraries,
            friendDependencies = emptyList(),
            analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration),
            icUseGlobalSignatures = false,
            icUseStdlibCache = false,
            icCache = emptyMap()
        )

        if (testFileIrReader != null) {
            val (moduleFragment: IrModuleFragment, _, _, _, _, _) =
                loadIr(sourceModule, IrFactoryImpl, true)
            testFileIrReader(moduleFragment.files.first { it.name.contains(FAKE_FILE_NAME) })
        }

        val result = compile(
            sourceModule,
            kotlinEnvironment.jsIrPhaseConfig,
            propertyLazyInitialization = false,
            mainArguments = arguments,
            irFactory = IrFactoryImpl
        )
        return result.outputs!!.jsCode
    }
}