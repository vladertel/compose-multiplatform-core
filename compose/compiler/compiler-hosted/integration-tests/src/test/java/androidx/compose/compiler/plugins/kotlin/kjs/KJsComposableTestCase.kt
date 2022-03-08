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

import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import java.util.AbstractMap
import java.util.function.Function
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.ir.declarations.IrFile


class KJsComposableTestCase {
    var waitForRecompositionCompleteContinuation: Continuation<Unit>? = null

    suspend fun waitForRecompositionComplete() {
        suspendCoroutine<Unit> { continuation ->
            waitForRecompositionCompleteContinuation = continuation
        }
    }

    var kotlinCodeBlock: () -> String = { "" }
        private set

    var applyOnJsCodeBlock: AbstractKJsCompileTest.OnJsSourceApplier.() -> Unit = {  }

    var verificationBlock: suspend CoroutineScope.(module: AbstractMap<Any, Any>) -> Unit = {}
        private set

    // helpers to view the ir and js code (mostly for debugging purposes)
    var onJsCodeReadyCallback: ((String) -> Unit)? = null
    var onIrReadyCallback: ((IrFile) -> Unit)? = null

    fun kotlinCode(block: () -> String) {
        kotlinCodeBlock = block
    }

    fun kotlinCode(@Language("kotlin") code:  String) {
        kotlinCodeBlock = { code }
    }

    fun applyOnJsCode(block: AbstractKJsCompileTest.OnJsSourceApplier.() -> Unit) {
        this.applyOnJsCodeBlock = block
    }

    fun verification(block: suspend CoroutineScope.(module: AbstractMap<Any, Any>) -> Unit) {
        verificationBlock = block
    }

    @Suppress("UNCHECKED_CAST")
    fun AbstractMap<Any, Any>.jsFunction(name: String): () -> Unit = {
        (get(name) as Function<Array<Any>, *>).apply(emptyArray())
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> AbstractMap<Any, Any>.jsFunctionWithResult(name: String): () -> R = {
        (get(name) as Function<Array<Any>, R>).apply(emptyArray())
    }
}