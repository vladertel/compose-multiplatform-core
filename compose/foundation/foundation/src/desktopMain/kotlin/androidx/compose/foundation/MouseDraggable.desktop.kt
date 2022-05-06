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

import androidx.compose.foundation.gestures.awaitPointerSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

data class DragChange(
    val offset: Offset = Offset.Zero,
    val previousKeyboardModifiers: PointerKeyboardModifiers,
    val currentKeyboardModifiers: PointerKeyboardModifiers
) {
    fun keyboardModifierChanged(): Boolean =
        previousKeyboardModifiers != currentKeyboardModifiers
}

fun Modifier.mouseDraggable(
    enabled: Boolean = true,
    buttons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    onDragStart: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDragCancel: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (DragChange) -> Unit
): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var dragInProgress by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var previousKeyboardModifiers by remember { mutableStateOf(PointerKeyboardModifiers()) }

    if (enabled) {
        Modifier.pointerInput(focusManager) {
            while (currentCoroutineContext().isActive) {
                dragInProgress = false
                awaitPointerEventScope {
                    var drag: PointerInputChange?
                    var overSlop = Offset.Zero
                    val press = awaitPress(
                        requireUnconsumed = false,
                        filterPressEvent = { e -> buttons(e.buttons) }
                    )
                    do {
                        drag = awaitPointerSlopOrCancellation(
                            press.changes[0].id,
                            press.changes[0].type
                        ) { change, over ->
                            change.consume()
                            overSlop = over
                        }
                    } while (drag != null && !drag.isConsumed)

                    if (drag != null && buttons(currentEvent.buttons)) {
                        val wasFocusedBefore = focused
                        if (!wasFocusedBefore) focusRequester.requestFocus()
                        dragInProgress = true
                        onDragStart(
                            press.changes[0].position,
                            currentEvent.keyboardModifiers
                        )
                        onDrag(
                            DragChange(overSlop, previousKeyboardModifiers, currentEvent.keyboardModifiers)
                        )
                        previousKeyboardModifiers = currentEvent.keyboardModifiers

                        val dragCompleted = drag(drag.id) {
                            val change = DragChange(
                                it.positionChange(),
                                previousKeyboardModifiers,
                                currentEvent.keyboardModifiers
                            )
                            previousKeyboardModifiers = currentEvent.keyboardModifiers
                            onDrag(change)
                            it.consume()
                        }

                        if (!dragCompleted) {
                            onDragCancel()
                        } else {
                            onDragEnd()
                        }
                        dragInProgress = false
                        if (focused && !wasFocusedBefore) focusManager.clearFocus() //TODO: Maybe keep focus
                    }
                }
            }
        }.onFocusChanged {
            focused = it.isFocused
        }.focusRequester(focusRequester)
            .onKeyEvent {
                if (dragInProgress && focused) {
                    val newKeyboardModifiers = it.toPointerKeyboardModifiers()
                    if (previousKeyboardModifiers != newKeyboardModifiers) {
                        onDrag(DragChange(Offset.Zero, previousKeyboardModifiers, newKeyboardModifiers))
                        previousKeyboardModifiers = newKeyboardModifiers
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }.focusable()
    } else {
        Modifier
    }
}

