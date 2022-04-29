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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private class MouseHandlerScope(
    pointerInputScope: PointerInputScope,
    val interactionSource: MutableInteractionSource,
    val pressedInteraction: MutableState<PressInteraction.Press?>,
    val filterMouseButtons: State<(PointerButtons) -> Boolean>,
    val onDoubleClick: State<((PointerKeyboardModifiers) -> Unit)?>,
    val onLongPress: State<((PointerKeyboardModifiers) -> Unit)?> ,
    val onClick: State<(PointerKeyboardModifiers) -> Unit>
) : PointerInputScope by pointerInputScope {

    init {
        println("Instantiate MouseHandlerScope")
    }

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

    suspend fun awaitMouseEvents() {
        coroutineScope {
            awaitPointerEventScope {
                pressScope.reset()

                val firstPress = awaitPress(filterMouseButtons.value).apply {
                    changes.forEach { it.consume() }
                }

                handlePressInteraction(firstPress.changes[0].position)

                var cancelled = false

                // `firstRelease` will be null if either event is cancelled or it's timed out
                // use `cancelled` flag to distinguish between two cases

                val firstRelease = withTimeoutOrNull(longPressTimeout) {
                    awaitReleaseOrCancelled(filterMouseButtons.value).apply {
                        this?.changes?.forEach { it.consume() }
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
                        onLongPress.value!!.invoke(firstPress.keyboardModifiers)
                        awaitReleaseOrCancelled(filterMouseButtons.value)
                        pressScope.release()
                    }
                } else if (onDoubleClick.value == null) {
                    onClick.value(firstPress.keyboardModifiers)
                } else {
                    processSecondClickIfAny(firstPress, firstRelease, this@coroutineScope)
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.processSecondClickIfAny(
        firstPress: PointerEvent,
        firstRelease: PointerEvent,
        coroutineScope: CoroutineScope,
    ) {
        pressScope.reset()

        val secondPress = awaitSecondPress(firstRelease.changes[0], filterMouseButtons.value)?.apply {
            changes.forEach { it.consume() }
        }

        if (secondPress == null) {
            onClick.value(firstPress.keyboardModifiers)
        } else {
            coroutineScope.handlePressInteraction(secondPress.changes[0].position)

            var cancelled = false

            val secondRelease = withTimeoutOrNull(longPressTimeout) {
                awaitReleaseOrCancelled(filterMouseButtons.value).apply {
                    this?.changes?.forEach { it.consume() }
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
                    onLongPress.value!!.invoke(secondPress.keyboardModifiers)
                    awaitReleaseOrCancelled(filterMouseButtons.value)
                    pressScope.release()
                }
            } else if (!cancelled) {
                onDoubleClick.value?.invoke(firstPress.keyboardModifiers)
            }
        }
    }

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

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    filterMouseButtons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    onDoubleClick: ((PointerKeyboardModifiers) -> Unit)? = null,
    onLongPress: ((PointerKeyboardModifiers) -> Unit)? = null,
    onClick: (PointerKeyboardModifiers) -> Unit
) = composed {
    val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }

    val onClickState = rememberUpdatedState(onClick)
    val on2xClickState = rememberUpdatedState(onDoubleClick)
    val onLongClickState = rememberUpdatedState(onLongPress)
    val filterState = rememberUpdatedState(filterMouseButtons)

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(interactionSource) {
            val mouseHandlerScope = MouseHandlerScope(
                pointerInputScope = this@pointerInput,
                interactionSource, pressedInteraction, filterState,
                on2xClickState, onLongClickState, onClickState
            )

            while (currentCoroutineContext().isActive) {
                mouseHandlerScope.awaitMouseEvents()
            }
        }
    } else {
        Modifier
    }

    gestureModifier.indication(interactionSource, indication)
}


@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    enabled: Boolean = true,
    filterMouseButtons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    onDoubleClick: ((PointerKeyboardModifiers) -> Unit)? = null,
    onLongPress: ((PointerKeyboardModifiers) -> Unit)? = null,
    onClick: (PointerKeyboardModifiers) -> Unit
) = composed {
    val indication = LocalIndication.current
    val interactionSource = remember { MutableInteractionSource() }
    Modifier.combinedMouseClickable(
        interactionSource, indication, enabled, filterMouseButtons, onDoubleClick, onLongPress, onClick
    )
}

