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
import isHeadlessBrowser
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement


class ComposeWindowLifecycleTest {
    private val canvasId = "canvas1"
    private val canvasId2 = "canvas2"


    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
        document.getElementById(canvasId2)?.remove()
    }

    @Test
    fun allEvents() {
        if (isHeadlessBrowser()) return
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        document.body!!.appendChild(canvas)

        val lifecycleOwner = ComposeWindow(
            canvas = canvas,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        // the ComposeWindow is not focused when created
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        canvas.focus()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        canvas.blur()
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        canvas.focus()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun twoComposeWindowsLifecycleShouldBeIndependent() {
        if (isHeadlessBrowser()) return
        val canvas1 = document.createElement("canvas") as HTMLCanvasElement
        canvas1.setAttribute("id", canvasId)
        document.body!!.appendChild(canvas1)

        val lifecycleOwner1 = ComposeWindow(
            canvas = canvas1,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        val canvas2 = document.createElement("canvas") as HTMLCanvasElement
        canvas2.setAttribute("id", canvasId2)
        document.body!!.appendChild(canvas2)

        val lifecycleOwner2 = ComposeWindow(
            canvas = canvas2,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        assertEquals(Lifecycle.State.STARTED, lifecycleOwner1.lifecycle.currentState)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner2.lifecycle.currentState)

        canvas1.focus()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner1.lifecycle.currentState)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner2.lifecycle.currentState)

        canvas2.focus()
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner1.lifecycle.currentState)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner2.lifecycle.currentState)

        canvas1.focus()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner1.lifecycle.currentState)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner2.lifecycle.currentState)

        canvas1.blur()
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner1.lifecycle.currentState)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner2.lifecycle.currentState)
    }
}