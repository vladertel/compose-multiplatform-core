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

package androidx.compose.foundation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isBack
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForward
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isPrimary
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondary
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiary
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.util.fastAll

@ExperimentalFoundationApi
class PointerFilterScope internal constructor(
    internal val event: PointerEvent
) {
    @OptIn(ExperimentalComposeUiApi::class)
    val button: PointerButton? = event.button
    val keyboardModifiers = event.keyboardModifiers

    val isMouse: Boolean = event.changes.fastAll { it.type == PointerType.Mouse }
    val isStylus: Boolean = event.changes.fastAll { it.type == PointerType.Stylus }
    val isTouch: Boolean = event.changes.fastAll { it.type == PointerType.Touch }
    val isEraser: Boolean = event.changes.fastAll { it.type == PointerType.Eraser }

    @OptIn(ExperimentalComposeUiApi::class)
    fun isButtonPressed(button: PointerButton): Boolean {
        return when (event.type) {
            PointerEventType.Press,
            PointerEventType.Release -> {
                this.button == button
            }
            // TODO: add a proper function for this purpose:
            else -> if (button.isPrimary) {
                event.buttons.isPrimaryPressed
            } else if (button.isSecondary) {
                event.buttons.isSecondaryPressed
            } else if (button.isTertiary) {
                event.buttons.isTertiaryPressed
            } else if (button.isBack) {
                event.buttons.isBackPressed
            } else if (button.isForward) {
                event.buttons.isForwardPressed
            } else {
                false
            }
        }
    }
}
