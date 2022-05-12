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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalKeyboardModifiers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive

@ExperimentalFoundationApi
data class DragChange(
    val offset: Offset = Offset.Zero,
    val previousKeyboardModifiers: PointerKeyboardModifiers,
    val currentKeyboardModifiers: PointerKeyboardModifiers
) {
    fun keyboardModifierChanged(): Boolean =
        previousKeyboardModifiers != currentKeyboardModifiers
}


@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
/**
 * @param filter - allows to filter press events which initialise the drag. By default, it expects
 * either [PointerButton.isPrimary] for mouse or usual tap for non-mouse pointers.
 * @param onDrag - receives an instance of [DragChange]  for every pointer position change when dragging or
 * when [PointerKeyboardModifiers] state changes after drag started and before it ends.
 */
fun Modifier.draggable(
    enabled: Boolean = true,
    filter: PointerFilterScope.() -> Boolean = { isMouse && button.isPrimary || !isMouse },
    onDragStart: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDragCancel: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (DragChange) -> Unit
): Modifier = composed {
    val keyModifiers = LocalKeyboardModifiers.current
    var dragInProgress by remember { mutableStateOf(false) }
    var previousKeyboardModifiers by remember { mutableStateOf(PointerKeyboardModifiers()) }

    if (enabled) {
        LaunchedEffect(keyModifiers) {
            keyModifiers.filter {
                dragInProgress && previousKeyboardModifiers != it
            }.collect {
                onDrag(DragChange(Offset.Zero, previousKeyboardModifiers, it))
                previousKeyboardModifiers = it
            }
        }

        Modifier.pointerInput(Unit) {
            while (currentCoroutineContext().isActive) {
                dragInProgress = false
                awaitPointerEventScope {
                    var drag: PointerInputChange?
                    var overSlop = Offset.Zero
                    val press = awaitPress(
                        requireUnconsumed = false,
                        filterPressEvent = filter
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

                    if (drag != null) {
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
                    }
                }
            }
        }
    } else {
        Modifier
    }
}

