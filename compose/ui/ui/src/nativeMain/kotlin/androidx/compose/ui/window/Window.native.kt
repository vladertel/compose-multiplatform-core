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
package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.macos.ComposeWindow

fun Window(
    title: String = "JetpackNativeWindow",
    content: @Composable () -> Unit = { }

) {
    AppWindow(
        title = title,
    ).show {
        content()
    }
}

class AppWindow(title: String = "JetpackNativeWindow") {
    internal val window = ComposeWindow()

    init {
        setTitle(title)
    }

    fun setTitle(title: String) {
        window.setTitle(title)
    }

    private fun onCreate(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        window.setContent(content)
    }

    /**
     * Shows a window with the given Compose content.
     *
     * @param parentComposition The parent composition reference to coordinate
     *        scheduling of composition updates.
     *        If null then default root composition will be used.
     * @param content Composable content of the window.
     */
    fun show(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        onCreate(parentComposition) {
            // window.layer.owners.keyboard = keyboard
            content()
        }
    }
}
