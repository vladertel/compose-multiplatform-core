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

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class ClicksHandlerScope(
    pointerInputScope: PointerInputScope,
    val interactionSource: MutableInteractionSource,
    val pressedInteraction: MutableState<PressInteraction.Press?>,
    val filterState: State<ClickFilterScope.() -> Boolean>,
    val onDoubleClick: State<(() -> Unit)?>,
    val onLongPress: State<(() -> Unit)?>,
    val onClick: State<() -> Unit>
) : PointerInputScope by pointerInputScope
{

    private val pressScope = PressGestureScopeImpl(this)

    private val longPressTimeout: Long
        get() = if (onLongPress.value != null) {
            viewConfiguration.longPressTimeoutMillis
        } else {
            Long.MAX_VALUE / 2
        }

    private fun CoroutineScope.handlePressInteraction(pressOffset: Offset) {
        launch {
            pressScope.handlePressInteraction(
                pressPoint = pressOffset,
                interactionSource = interactionSource,
                pressedInteraction = pressedInteraction,
                delayPressInteraction = mutableStateOf({ false })
            )
        }
    }

    suspend fun awaitEvents() {
        coroutineScope {
            awaitPointerEventScope {
                pressScope.reset()

                val firstPress = awaitPress().apply {
                    changes.fastForEach { it.consume() }
                }

                handlePressInteraction(firstPress.changes[0].position)

                var cancelled = false

                // `firstRelease` will be null if either event is cancelled or it's timed out
                // use `cancelled` flag to distinguish between two cases

                val firstRelease = withTimeoutOrNull(longPressTimeout) {
                    awaitReleaseOrCancelled().apply {
                        this?.changes?.fastForEach { it.consume() }
                        cancelled = this == null
                    }
                }

                if (cancelled) {
                    pressScope.cancel()
                } else if (firstRelease != null) {
                    pressScope.release()
                }

                if (firstRelease == null) {
                    if (onLongPress.value != null && !cancelled) {
                        onLongPress.value!!.invoke()
                        awaitReleaseOrCancelled()
                        pressScope.release()
                    }
                } else if (onDoubleClick.value == null) {
                    onClick.value()
                } else {
                    processSecondClickIfAny(firstRelease, this@coroutineScope)
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.processSecondClickIfAny(
        firstRelease: PointerEvent,
        coroutineScope: CoroutineScope,
    ) {
        pressScope.reset()

        val secondPress = awaitSecondPressUnconsumed(
            firstRelease.changes[0]
        )?.apply {
            changes.fastForEach { it.consume() }
        }

        if (secondPress == null) {
            onClick.value()
        } else {
            coroutineScope.handlePressInteraction(secondPress.changes[0].position)

            var cancelled = false

            val secondRelease = withTimeoutOrNull(longPressTimeout) {
                awaitReleaseOrCancelled().apply {
                    this?.changes?.fastForEach { it.consume() }
                    cancelled = this == null
                }
            }

            if (cancelled) {
                pressScope.cancel()
            } else if (secondRelease != null) {
                pressScope.release()
            }

            if (secondRelease == null) {
                if (onLongPress.value != null && !cancelled) {
                    onLongPress.value!!.invoke()
                    awaitReleaseOrCancelled()
                    pressScope.release()
                }
            } else if (!cancelled) {
                onDoubleClick.value?.invoke()
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitPress(): PointerEvent {
        return awaitPress(filterState.value)
    }

    private suspend fun AwaitPointerEventScope.awaitSecondPressUnconsumed(
        firstUp: PointerInputChange
    ): PointerEvent? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var event: PointerEvent
        var change: PointerInputChange
        // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
        do {
            event = awaitPress()
            change = event.changes[0]
        } while (change.uptimeMillis < minUptime)
        event
    }

    private suspend fun AwaitPointerEventScope.awaitReleaseOrCancelled(): PointerEvent? {
        var event: PointerEvent? = null
        var changes: List<PointerInputChange> = emptyList()

        while (event == null || changes.isEmpty()) {
            event = awaitPointerEvent()

            val cancelled = event.changes.fastAny {
                it.isOutOfBounds(size, Size.Zero)
            }

            if (cancelled) return null

            event = event.takeIf {
                it.type == PointerEventType.Release &&
                    filterState.value(ClickFilterScope(it))
            }

            changes = event?.changes?.takeIf {
                it.all { !it.isConsumed }
            } ?: emptyList()

            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the Main pass we checked above.
            val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
            if (consumeCheck.changes.fastAny { it.isConsumed }) {
                return null
            }
        }

        return event
    }

    private class PressGestureScopeImpl(
        density: Density
    ) : PressGestureScope, Density by density {
        private var isReleased = false
        private var isCanceled = false
        private val mutex = Mutex(locked = false)

        /**
         * Called when a gesture has been canceled.
         */
        fun cancel() {
            isCanceled = true
            mutex.unlock()
        }

        /**
         * Called when all pointers are up.
         */
        fun release() {
            isReleased = true
            mutex.unlock()
        }

        /**
         * Called when a new gesture has started.
         */
        fun reset() {
            mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
            isReleased = false
            isCanceled = false
        }

        override suspend fun awaitRelease() {
            if (!tryAwaitRelease()) {
                throw GestureCancellationException("The press gesture was canceled.")
            }
        }

        override suspend fun tryAwaitRelease(): Boolean {
            if (!isReleased && !isCanceled) {
                mutex.lock()
            }
            return isReleased
        }
    }
}

internal suspend fun AwaitPointerEventScope.awaitPress(
    filterPressEvent: ClickFilterScope.() -> Boolean,
    requireUnconsumed: Boolean = true
): PointerEvent {
    var event: PointerEvent? = null
    var changes: List<PointerInputChange> = emptyList()

    while (event == null || changes.isEmpty()) {
        event = awaitPointerEvent().takeIf {
                it.type == PointerEventType.Press &&
                filterPressEvent(ClickFilterScope(it))
        }
        changes = event?.changes?.takeIf {
            !requireUnconsumed || it.fastAll { !it.isConsumed }
        } ?: emptyList()
    }

    return event
}

internal fun KeyEvent.toPointerKeyboardModifiers(): PointerKeyboardModifiers = PointerKeyboardModifiers(
    isCtrlPressed = this.isCtrlPressed,
    isMetaPressed = this.isMetaPressed,
    isAltPressed = this.isAltPressed,
    isAltGraphPressed = this.isAltPressed,
    isShiftPressed = this.isShiftPressed,
    // TODO: add implementations
    isSymPressed = false,
    isCapsLockOn = false,
    isFunctionPressed = false,
    isScrollLockOn = false,
    isNumLockOn = false
)
