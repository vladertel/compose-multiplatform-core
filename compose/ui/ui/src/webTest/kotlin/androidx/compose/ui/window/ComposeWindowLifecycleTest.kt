/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.lifecycle.Lifecycle
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLCanvasElement


class ComposeWindowLifecycleTest {
    private val canvasId = "canvas1"

    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
    }

    @Test
    fun allEvents() = runTest {
        if (isHeadlessBrowser()) return@runTest
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        canvas.setAttribute("tabindex", "0")

        document.body!!.appendChild(canvas)
        canvas.focus()

        val lifecycleOwner = ComposeWindow(
            canvas = canvas,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        // Browsers don't allow to blur the window from code:
        // https://developer.mozilla.org/en-US/docs/Web/API/Window/blur
        // So we simulate a new tab being open:
        val anotherWindow = window.open("http://localhost:80")
        assertTrue(anotherWindow != null)

        realDelay(100)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        // Now go back to the original window
        anotherWindow.close()

        realDelay(100)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)
    }
}

private suspend fun TestScope.realDelay(ms: Long) {
    // To make sure the delay is not skipped by TestScheduler, we change the Dispatcher
    withContext(Dispatchers.Default) {
        delay(100)
    }
}