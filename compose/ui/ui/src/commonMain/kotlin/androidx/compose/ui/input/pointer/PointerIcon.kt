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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalPointerIconService

/**
 * Represents a pointer icon to use in [Modifier.pointerHoverIcon]
 */
@ExperimentalComposeUiApi
interface PointerIcon {
    companion object {
        val Default = pointerIconDefault
        val Crosshair = pointerIconCrosshair
        val Text = pointerIconText
        val Hand = pointerIconHand
    }
}

internal expect val pointerIconDefault: PointerIcon
internal expect val pointerIconCrosshair: PointerIcon
internal expect val pointerIconText: PointerIcon
internal expect val pointerIconHand: PointerIcon

internal interface PointerIconService {
    var current: PointerIcon
}

/**
 * Creates modifier which specifies desired pointer icon when the cursor is over the modified
 * element.
 *
 * @param icon The icon to set
 * @param enforce if true parent's PointerIcon overrides children's. To set
 * "default" PointerIcon that could be overridden by children set enforce to false.
 */
@ExperimentalComposeUiApi
fun Modifier.pointerHoverIcon(icon: PointerIcon, enforce: Boolean = true) =
    composed {
        val pointerIconService = LocalPointerIconService.current
        if (pointerIconService == null) {
            Modifier
        } else {
            this.pointerInput(icon, enforce) {
                awaitPointerEventScope {
                    while (true) {
                        val pass = if (enforce) PointerEventPass.Main else PointerEventPass.Initial
                        val event = awaitPointerEvent(pass)
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> {
                                pointerIconService.current = icon
                            }
                        }
                    }
                }
            }
        }
    }
