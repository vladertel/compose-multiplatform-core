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

package androidx.compose.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.currentCoroutineContext

private suspend fun AwaitPointerEventScope.awaitPress(
    filter: (PointerButtons) -> Boolean
): PointerEvent {
    var event: PointerEvent? = null
    var changes: List<PointerInputChange> = emptyList()

    while (event == null || !filter(event.buttons) || changes.isEmpty()) {
        event = awaitPointerEvent().takeIf { it.type == PointerEventType.Press }
        changes = event?.changes?.takeIf {
            it.all { it.type == PointerType.Mouse && !it.isConsumed }
        } ?: emptyList()
    }

    return event
}

private suspend fun AwaitPointerEventScope.awaitSecondPress(
    firstUp: PointerInputChange,
    filter: (PointerButtons) -> Boolean
): PointerEvent? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var event: PointerEvent
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        event = awaitPress(filter)
        change = event.changes[0]
    } while (change.uptimeMillis < minUptime)
    event
}

private suspend fun AwaitPointerEventScope.awaitReleaseOrCancelled(filter: (PointerButtons) -> Boolean): PointerEvent? {
    var event: PointerEvent? = null
    var changes: List<PointerInputChange> = emptyList()

    while (event == null || filter(event.buttons) || changes.isEmpty()) {
        event = awaitPointerEvent()

        val cancelled = event.changes.any {
            it.type == PointerType.Mouse && it.isOutOfBounds(size, Size.Zero)
        }

        if (cancelled) return null

        event = event.takeIf { it.type == PointerEventType.Release }

        changes = event?.changes?.takeIf {
            it.all { it.type == PointerType.Mouse && !it.isConsumed }
        } ?: emptyList()
    }

    return event
}

private suspend fun AwaitPointerEventScope.processSecondClickIfAny(
    firstPress: PointerEvent,
    firstRelease: PointerEvent,
    filterMouseButtons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    onDoubleClick: ((PointerKeyboardModifiers) -> Unit),
    onLongPress: ((PointerKeyboardModifiers) -> Unit)? = null,
    onClick: (PointerKeyboardModifiers) -> Unit
) {
    val secondPress = awaitSecondPress(firstRelease.changes[0], filterMouseButtons)?.apply {
        changes.forEach { it.consume() }
    }

    if (secondPress == null) {
        onClick(firstPress.keyboardModifiers)
    } else {
        var cancelled = false

        val longPressTimeout = if (onLongPress != null) {
            viewConfiguration.longPressTimeoutMillis
        } else {
            Long.MAX_VALUE / 2
        }

        val secondRelease = withTimeoutOrNull(longPressTimeout) {
            awaitReleaseOrCancelled(filterMouseButtons).apply {
                this?.changes?.forEach { it.consume() }
                cancelled = this == null
            }
        }

        if (secondRelease == null) {
            if (onLongPress != null && !cancelled) {
                onLongPress.invoke(secondPress.keyboardModifiers)
                awaitReleaseOrCancelled(filterMouseButtons)
            }
        } else if (!cancelled) {
            onDoubleClick(firstPress.keyboardModifiers)
        }
    }
}

fun Modifier.combinedMouseClickable(
    filterMouseButtons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    onDoubleClick: ((PointerKeyboardModifiers) -> Unit)? = null,
    onLongPress: ((PointerKeyboardModifiers) -> Unit)? = null,
    onClick: (PointerKeyboardModifiers) -> Unit
) = this.pointerInput(Unit) {
    while(currentCoroutineContext().isActive) {
        coroutineScope {
            awaitPointerEventScope {
                val firstPress = awaitPress(filterMouseButtons).apply {
                    changes.forEach { it.consume() }
                }

                val longPressTimeout = if (onLongPress != null) {
                    viewConfiguration.longPressTimeoutMillis
                } else {
                    Long.MAX_VALUE / 2
                }

                var cancelled = false

                // `firstRelease` will be null if either event is cancelled or it's timed out
                // use `cancelled` flag to distinguish between two cases

                val firstRelease = withTimeoutOrNull(longPressTimeout) {
                    awaitReleaseOrCancelled(filterMouseButtons).apply {
                        this?.changes?.forEach { it.consume() }
                        cancelled = this == null
                    }
                }

                if (firstRelease == null) {
                    if (onLongPress != null && !cancelled) {
                        onLongPress.invoke(firstPress.keyboardModifiers)
                        awaitReleaseOrCancelled(filterMouseButtons)
                    }
                } else if (onDoubleClick == null) {
                    onClick(firstPress.keyboardModifiers)
                } else {
                    processSecondClickIfAny(
                        firstPress, firstRelease, filterMouseButtons, onDoubleClick, onLongPress, onClick
                    )
                }
            }
        }
    }
}