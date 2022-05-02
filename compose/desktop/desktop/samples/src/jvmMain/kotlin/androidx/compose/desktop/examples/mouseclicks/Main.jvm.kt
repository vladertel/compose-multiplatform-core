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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.foundation.combinedMouseClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.Checkbox
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    singleWindowApplication(
        title = "Desktop Mouse Clicks",
        state = WindowState(width = 1024.dp, height = 850.dp)
    ) {
        val checked = remember { mutableStateOf(true) }


        Column {
            Box(
                modifier = Modifier.size(200.dp).background(Color.Red)
//                    .clickable {  }
//                    .combinedClickable {  }
//                    .mouseClickable {  }
                    .combinedMouseClickable(buttons = { it.isSecondaryPressed }) {
                        println("RIGHT CLICK")
                    }
                    .combinedMouseClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        enabled = checked.value,
                        buttons = {
                            it.isPrimaryPressed
                        },
                        keyModifiers = {
                            true
                            //it.isShiftPressed
                                       },
                        onLongPress = {
                            println("Left LongPress ()")
                        },
//                        onDoubleClick = {
//                            println("Left 2xClick ()")
//                        }
                    ) {
                        println("Left Click ()")
                    }
            ) {

                Box(
                    modifier = Modifier.padding(25.dp).size(150.dp).background(Color.Green)
                        .combinedMouseClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            enabled = checked.value,
                            buttons = {
                                it.isPrimaryPressed
                            },
                            onLongPress = {
                                println("Inner: Left LongPress ()")
                            },
//                            onDoubleClick = { modifiers ->
//                                println("Inner: Left 2xClick ($modifiers)")
//                            }
                        ) {
                            println("Inner: Left Click ()")
                        }
                )

            }


            Checkbox(checked.value, onCheckedChange = {
                checked.value = it
            })
        }
    }
}
