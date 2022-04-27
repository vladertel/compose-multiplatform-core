/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.desktop.examples.mouseclicks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.desktop.*
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed

fun main() {
    singleWindowApplication(
        title = "Desktop Mouse Clicks",
        state = WindowState(width = 1024.dp, height = 850.dp)
    ) {
        Box(
            modifier = Modifier.size(200.dp).background(Color.Red)
                .combinedMouseClickable(
                    filterMouseButtons = {
                        it.isPrimaryPressed
                    },
                    onLongPress = { modifiers ->
                        println("Left LongPress ($modifiers)")
                    },
                    onDoubleClick = { modifiers ->
                        println("Left 2xClick ($modifiers)")
                    }
                ) { modifiers ->
                    println("Left Click ($modifiers)")
                }
                .combinedMouseClickable(
                    filterMouseButtons = {
                        it.isSecondaryPressed
                    },
                    onLongPress = { modifiers ->
                        println("Right LongPress ($modifiers)")
                    },
                    onDoubleClick = { modifiers ->
                        println("Right 2xClick ($modifiers)")
                    }
                ) { modifiers ->
                    println("Right Click ($modifiers)")
                }
        )
    }
}
