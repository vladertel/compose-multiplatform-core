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

import androidx.compose.runtime.Composable
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.function.Function
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.junit.Ignore
import org.junit.Test

class TestSimpleComposablesInKJS : AbstractKJsCompileTest() {

    @Test // just to make sure that failed assertions in verification block can be caught
    fun shouldFail() {
        var failed = false
        try {
            kjsComposableTest {
                kotlinCode { "" }
                verification {
                    assertEquals(0, 1)
                }
            }
        } catch (e: AssertionError) {
            failed = true
        }

        assertTrue("expected AssertionError", failed)
    }

    @Test
    fun callingDecoyShouldFail() {
        val error = kjsComposableTestShouldFail {
            kotlinCode {
                """
                @Composable @JsExport
                fun AbcComposable() {}
            """.trimIndent()
            }

            applyOnJsCode {
                export("AbcComposable") // decoys don't have @JsExport annotation
            }

            verification { module ->
                val abcComposableDecoy = module.jsFunction("AbcComposable")
                abcComposableDecoy()
            }
        }

        assertTrue(
            error.message!!.contains(
                "Function AbcComposable should have been replaced by compiler"
            )
        )
    }


    @Test
    fun testSimpleComposable() = kjsComposableTest {
        kotlinCode {
            """
            val state = mutableStateOf(0)
            
            var abcCounter = 0
                        
            @Composable
            fun AbcComposable() {
                val readState = state.value
                abcCounter++
            }
            
            @JsExport
            fun runTestComposition() {
                createComposition().setContent {
                    AbcComposable()
                }
            }
            
            @JsExport
            fun forceUpdateState() {
                state.value = state.value + 1
            }
            """.trimIndent()
        }

        applyOnJsCode {
            exportGetterFor("abcCounter")
        }

        verification { module ->
            val runTestComposition = module.jsFunction("runTestComposition")
            val updateStateJsFun = module.jsFunction("forceUpdateState")
            val readAbcCounter = module.jsFunctionWithResult<Int>("get_abcCounter")

            assertEquals(0, readAbcCounter())

            runTestComposition()
            assertEquals(1, readAbcCounter())

            repeat(3) {
                updateStateJsFun()
                waitForRecompositionComplete()
                assertEquals(it + 2, readAbcCounter())
            }
        }
    }

    @Test
    fun callLocalComposableFunctionAndInvalidate() = kjsComposableTest {
        kotlinCode {
            """
                var innerCalled = 0
                var innerScope: RecomposeScope? = null
                
                @Composable
                fun Test() {
                    @Composable
                    fun inner() {
                        innerScope = currentRecomposeScope
                        innerCalled++ 
                    }
                    inner()
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        Test()
                    }
                }
                
                @JsExport
                fun invalidateInnerScope() {
                    innerScope?.invalidate()
                }
            """.trimIndent()
        }

        applyOnJsCode {
            exportGetterFor("innerCalled")
        }

        verification { module ->
            val runTestComposition = module.jsFunction("runTestComposition")
            val currentInnerCalled = module.jsFunctionWithResult<Int>("get_innerCalled")
            val invalidateInnerScope = module.jsFunction("invalidateInnerScope")

            runTestComposition()
            assertEquals(1, currentInnerCalled())

            invalidateInnerScope()
            waitForRecompositionComplete()
            assertEquals(2, currentInnerCalled())

            invalidateInnerScope()
            waitForRecompositionComplete()
            assertEquals(3, currentInnerCalled())
        }
    }

    @Test
    fun callingAWrapperComposableJS() = kjsComposableTest {
        kotlinCode {
            """
                var testCallCounter = 0
                var wCallCounter = 0
                var aCallCounter = 0
                var contentCallCounter = 0
                
                var scope: RecomposeScope? = null

                @Composable 
                fun W(content: @Composable () -> Unit) {
                    wCallCounter++
                    content() 
                }

                @Composable fun A() {
                    aCallCounter++
                }
                
                @Composable
                fun Test() {
                    testCallCounter++
                    scope = currentRecomposeScope
                    W { 
                        contentCallCounter++
                        A() 
                    }
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        Test()
                    }
                }
                
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        }

        applyOnJsCode {
            exportGetterFor(
                "testCallCounter", "wCallCounter", "aCallCounter", "contentCallCounter"
            )
        }

        verification { module ->
            val testCallCounter = module.jsFunctionWithResult<Int>("get_testCallCounter")
            val wCallCounter = module.jsFunctionWithResult<Int>("get_wCallCounter")
            val aCallCounter = module.jsFunctionWithResult<Int>("get_aCallCounter")
            val contentCallCounter = module.jsFunctionWithResult<Int>("get_contentCallCounter")
            val runTestComposition = module.jsFunction("runTestComposition")
            val invalidateScope = module.jsFunction("invalidateScope")

            runTestComposition()

            val allCounters = listOf(
                testCallCounter, wCallCounter, aCallCounter, contentCallCounter
            ).map { it() }
            assertTrue(allCounters.joinToString(), allCounters.all { it == 1 })

            invalidateScope()
            waitForRecompositionComplete()

            assertEquals(2, testCallCounter())

            val allButTestCountersEq1 = listOf(
                wCallCounter, aCallCounter, contentCallCounter
            ).all {
                it() == 1
            }

            assertTrue(allButTestCountersEq1)
        }
    }
}