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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun testDecoyAnonymousObjectInterfaceImpl() = kjsComposableTest {
        kotlinCode {
            """
                var testCallCounter = 0
                var scope: RecomposeScope? = null
                
                interface Something {
                    @Composable fun test()
                }
                val field = object : Something {
                    @Composable
                    override fun test() {
                        testCallCounter++
                    }
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        scope = currentRecomposeScope
                        field.test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        }
        applyOnJsCode {
            exportGetterFor("testCallCounter")
        }
        verification { module ->
            val testCallCounter = module.jsFunctionWithResult<Int>("get_testCallCounter")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            runTestComposition()
            assertEquals(1, testCallCounter())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(2, testCallCounter())
        }
    }

    @Test
    fun testDecoyComposableWithDefaultLambda() = kjsComposableTest {
        kotlinCode {
            """
                var testCallCounter = 0
                var testCallRootCounter = 0
                var scope: RecomposeScope? = null
                
                @Composable
                fun Something(f: @Composable (Int) -> Unit = { testCallCounter++ }) {
                    f(0)
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        scope = currentRecomposeScope
                        testCallRootCounter++
                        Something()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        }
        applyOnJsCode {
            exportGetterFor("testCallCounter", "testCallRootCounter")
        }
        verification { module ->
            val testCallCounter = module.jsFunctionWithResult<Int>("get_testCallCounter")
            val testCallRootCounter = module.jsFunctionWithResult<Int>("get_testCallRootCounter")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            runTestComposition()
            assertEquals(1, testCallCounter())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(1, testCallCounter()) // 1 because recomposition is skipped
            assertEquals(2, testCallRootCounter())
        }
    }

    @Test
    fun testDecoyComposableWithDefaultParameter() = kjsComposableTest {
        kotlinCode {
            """
                var somethingSum = 0
                var rootCallCounter = 0
                var scope: RecomposeScope? = null
                
                @Composable
                fun Something(i: Int = 5) {
                    somethingSum += i
                }
    
                @Composable
                fun callSomething() {
                    Something()
                    Something(100)
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        rootCallCounter++
                        scope = currentRecomposeScope
                        callSomething()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        }
        applyOnJsCode {
            exportGetterFor("somethingSum", "rootCallCounter")
        }
        verification { module ->
            val somethingSum = module.jsFunctionWithResult<Int>("get_somethingSum")
            val rootCallCounter = module.jsFunctionWithResult<Int>("get_rootCallCounter")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, somethingSum())
            runTestComposition()
            assertEquals(105, somethingSum())
            assertEquals(1, rootCallCounter())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(105, somethingSum()) // recomposition is skipped
            assertEquals(2, rootCallCounter())
        }
    }

    @Test
    fun testDecoyConstructor() = kjsComposableTest {
        kotlinCode {
            """
                var callCounter = 0
                var scope: RecomposeScope? = null
                
                class Test(val param: @Composable () -> Unit) {
                    constructor(i: Int, param: @Composable () -> Unit) : this(param)
                }
                
                @JsExport
                fun runTestComposition() {
                    val t = Test(0) {
                        scope = currentRecomposeScope
                        callCounter++
                    }
                    createComposition().setContent {
                        t.param()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        }
        applyOnJsCode {
            exportGetterFor("callCounter")
        }
        verification { module ->
            val callCounter = module.jsFunctionWithResult<Int>("get_callCounter")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callCounter())
            runTestComposition()
            assertEquals(1, callCounter())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(2, callCounter())
        }
    }

    @Test
    fun testDecoyVarargParam() = kjsComposableTest {
        kotlinCode {
            """
                var argsConcat = ""
                
                @Composable
                fun onCommit(vararg inputs: Any?) {
                    argsConcat = inputs?.joinToString(",") ?: ""
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        onCommit("A", "B", "C", 3)
                    }
                }
            """.trimIndent()
        }
        applyOnJsCode {
            exportGetterFor("argsConcat")
        }
        verification { module ->
            val argsConcatResult = module.jsFunctionWithResult<String>("get_argsConcat")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals("", argsConcatResult())
            runTestComposition()
            assertEquals("A,B,C,3", argsConcatResult())
        }
    }

    @Test
    fun testDecoyComposableInProperty() = kjsComposableTest {
        kotlinCode(
            """
                var callCounter = 0
                
                private var PropComposable: @Composable () -> Unit = { callCounter++ }

                fun retPropComposable() = PropComposable
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        retPropComposable().invoke()
                    }
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callCounter")
        }
        verification { module ->
            val callCounter = module.jsFunctionWithResult<String>("get_callCounter")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callCounter())
            runTestComposition()
            assertEquals(1, callCounter())
        }
    }

    @Test
    fun testDecoyReceiverComposable() = kjsComposableTest {
        kotlinCode(
            """
                var callCounter = 0
                
                @NonRestartableComposable
                @Composable
                fun (@Composable () -> Unit).a() { this() }

                @Composable
                fun b() {
                    val something: (@Composable () -> Unit) = {
                        callCounter++
                    }
                    something.a()
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        b()
                    }
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callCounter")
        }
        verification { module ->
            val callCounter = module.jsFunctionWithResult<String>("get_callCounter")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callCounter())
            runTestComposition()
            assertEquals(1, callCounter())
        }
    }

    @Test
    fun testDecoyWithReturnComposable() = kjsComposableTest {
        kotlinCode(
            """
                var callCounter = 0
                var scope: RecomposeScope? = null
                
                @Composable
                fun a(): @Composable () -> Unit {
                  return { callCounter++ }
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        scope = currentRecomposeScope
                        a().invoke()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callCounter")
        }
        verification { module ->
            val callCounter = module.jsFunctionWithResult<String>("get_callCounter")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callCounter())
            runTestComposition()
            assertEquals(1, callCounter())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(1, callCounter())
        }
    }

    @Test
    fun testDecoySameName() = kjsComposableTest {
        kotlinCode(
            """
                var callSum = 0
                var scope: RecomposeScope? = null
                
                @Composable
                @NonRestartableComposable
                fun a() { callSum += 1 }

                @Composable
                @NonRestartableComposable
                fun a(param: Int) { callSum *= param}

                @Composable
                @NonRestartableComposable
                fun test() {
                    scope = currentRecomposeScope
                    a()
                    a(param = 5)
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callSum")
        }
        verification { module ->
            val callSum = module.jsFunctionWithResult<String>("get_callSum")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callSum())
            runTestComposition()
            assertEquals(5, callSum())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(30, callSum())
        }
    }

    @Test
    fun testDecoysInterfaceCall() = kjsComposableTest {
        kotlinCode(
            """
                var callSum = 0
                var scope: RecomposeScope? = null
                
                interface IntroFace {
                    @Composable
                    fun <T> a(): T?
                }

                @NonRestartableComposable
                @Composable
                fun a(value: IntroFace) {
                    callSum += value.a() ?: 0
                }

                val obj = object : IntroFace {
                    @Composable
                    override fun <T> a(): T? {
                        return 1 as T
                    }
                } 

                @Composable
                @NonRestartableComposable
                fun test() {
                    scope = currentRecomposeScope
                    a(obj)
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callSum")
        }
        verification { module ->
            val callSum = module.jsFunctionWithResult<String>("get_callSum")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callSum())
            runTestComposition()
            assertEquals(1, callSum())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(2, callSum())
        }
    }

    @Test
    fun testDecoysEnclosingClassTypeParameter() = kjsComposableTest {
        kotlinCode(
            """
                var callSum = 0
                var scope: RecomposeScope? = null

                class Parent<T> {
                    @Composable 
                    fun myFunction(param: Boolean, value: T): T? {
                        if (param) {
                            return value
                        } else {
                            return null
                        }
                    }
                }
            
                val instance = Parent<Int>()

                @Composable
                @NonRestartableComposable
                fun test() {
                    scope = currentRecomposeScope
                    callSum += instance.myFunction(true, 5) ?: 0
                    callSum *= instance.myFunction(false, 50) ?: 2
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callSum")
        }
        verification { module ->
            val callSum = module.jsFunctionWithResult<String>("get_callSum")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callSum())
            runTestComposition()
            assertEquals(10, callSum())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(30, callSum())
        }
    }

    @Test
    fun testDecoysTypeParameter() = kjsComposableTest {
        kotlinCode(
            """
                var callSum = 0
                var scope: RecomposeScope? = null

                @Composable 
                fun <T> myFunction(param: Boolean, value: T): T? {
                    if (param) {
                        return value
                    } else {
                        return null
                    }
                }

                @Composable
                @NonRestartableComposable
                fun test() {
                    scope = currentRecomposeScope
                    callSum += myFunction(true, 5) ?: 0
                    callSum *= myFunction(false, 50) ?: 2
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callSum")
        }
        verification { module ->
            val callSum = module.jsFunctionWithResult<String>("get_callSum")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callSum())
            runTestComposition()
            assertEquals(10, callSum())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(30, callSum())
        }
    }

    @Test
    fun testDecoysPropertyGetterSubstitution() = kjsComposableTest {
        kotlinCode(
            """
                var callSum = 0
                var scope: RecomposeScope? = null

                val someProperty: Int @Composable get() = 100

                @NonRestartableComposable
                @Composable fun myFunction() {
                    callSum += someProperty
                }

                @Composable
                @NonRestartableComposable
                fun test() {
                    scope = currentRecomposeScope
                    myFunction()
                }
                
                @JsExport
                fun runTestComposition() {
                    createComposition().setContent {
                        test()
                    }
                }
                @JsExport
                fun invalidateScope() {
                    scope?.invalidate()
                }
            """.trimIndent()
        )
        applyOnJsCode {
            exportGetterFor("callSum")
        }
        verification { module ->
            val callSum = module.jsFunctionWithResult<String>("get_callSum")
            val invalidateScope = module.jsFunction("invalidateScope")
            val runTestComposition = module.jsFunction("runTestComposition")

            assertEquals(0, callSum())
            runTestComposition()
            assertEquals(100, callSum())

            invalidateScope()
            waitForRecompositionComplete()
            assertEquals(200, callSum())
        }
    }
}