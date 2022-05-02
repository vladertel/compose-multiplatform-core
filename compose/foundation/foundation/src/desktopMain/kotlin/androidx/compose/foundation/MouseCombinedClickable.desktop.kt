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
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    val filterKeyboardModifiers: State<(PointerKeyboardModifiers) -> Boolean>,
    val onDoubleClick: State<(() -> Unit)?>,
    val onLongPress: State<(() -> Unit)?>,
    val onClick: State<() -> Unit>
) : PointerInputScope by pointerInputScope {

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

                val firstPress = awaitPress().apply {
                    changes.forEach { it.consume() }
                }

                handlePressInteraction(firstPress.changes[0].position)

                var cancelled = false

                // `firstRelease` will be null if either event is cancelled or it's timed out
                // use `cancelled` flag to distinguish between two cases

                val firstRelease = withTimeoutOrNull(longPressTimeout) {
                    awaitReleaseOrCancelled().apply {
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
                        onLongPress.value!!.invoke()
                        awaitReleaseOrCancelled()
                        pressScope.release()
                    }
                } else if (onDoubleClick.value == null) {
                    onClick.value()
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

        val secondPress = awaitSecondPress(
            firstRelease.changes[0]
        )?.apply {
            changes.forEach { it.consume() }
        }

        if (secondPress == null) {
            onClick.value()
        } else {
            coroutineScope.handlePressInteraction(secondPress.changes[0].position)

            var cancelled = false

            val secondRelease = withTimeoutOrNull(longPressTimeout) {
                awaitReleaseOrCancelled().apply {
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
        var event: PointerEvent? = null
        var changes: List<PointerInputChange> = emptyList()

        while (event == null || changes.isEmpty()) {
            event = awaitPointerEvent().takeIf {
                it.type == PointerEventType.Press && filterMouseButtons.value(it.buttons) && filterKeyboardModifiers.value(
                    it.keyboardModifiers
                )
            }
            changes = event?.changes?.takeIf {
                it.all { it.type == PointerType.Mouse && !it.isConsumed }
            } ?: emptyList()
        }

        return event
    }

    private suspend fun AwaitPointerEventScope.awaitSecondPress(
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

        while (event == null || filterMouseButtons.value(event.buttons) || changes.isEmpty()) {
            event = awaitPointerEvent()

            val cancelled = event.changes.any {
                it.type == PointerType.Mouse && it.isOutOfBounds(size, Size.Zero)
            } || !filterKeyboardModifiers.value(event.keyboardModifiers)

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

data class CombinedMouseClickableLabels(
    val onDoubleClickLabel: String? = null,
    val onLongPressLabel: String? = null,
    val onClickLabel: String? = null
)

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedMouseClickableLabels? = null,
    buttons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    keyModifiers: (PointerKeyboardModifiers) -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "combinedMouseClickable"
        properties["enabled"] = enabled
        properties["buttons"] = buttons
        properties["keyModifiers"] = keyModifiers
        properties["role"] = role
        properties["labels"] = labels
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongPress"] = onLongPress
        properties["onClick"] = onClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    },
    factory = {
        val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }

        val onClickState = rememberUpdatedState(onClick)
        val on2xClickState = rememberUpdatedState(onDoubleClick)
        val onLongClickState = rememberUpdatedState(onLongPress)
        val filterState = rememberUpdatedState(buttons)
        val modifiersFilterState = rememberUpdatedState(keyModifiers)

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(interactionSource) {
                val mouseHandlerScope = MouseHandlerScope(
                    pointerInputScope = this@pointerInput,
                    interactionSource,
                    pressedInteraction,
                    filterState,
                    modifiersFilterState,
                    on2xClickState,
                    onLongClickState,
                    onClickState
                )

                while (currentCoroutineContext().isActive) {
                    mouseHandlerScope.awaitMouseEvents()
                }
            }
        } else {
            Modifier
        }

        gestureModifier
            .indication(interactionSource, indication)
            .semantics(mergeDescendants = true) {
                if (role != null) this.role = role
                this.onClick(labels?.onClickLabel) { onClick(); true }

                if (onLongPress != null) {
                    this.onLongClick(labels?.onLongPressLabel) { onLongPress(); true }
                }

                if (!enabled) disabled()
            }
    }
)


@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    enabled: Boolean = true,
    buttons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    keyModifiers: (PointerKeyboardModifiers) -> Boolean = { true },
    role: Role? = null,
    labels: CombinedMouseClickableLabels? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "combinedMouseClickable"
        properties["enabled"] = enabled
        properties["buttons"] = buttons
        properties["keyModifiers"] = keyModifiers
        properties["role"] = role
        properties["labels"] = labels
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongPress"] = onLongPress
        properties["onClick"] = onClick
    }, factory = {
        val indication = LocalIndication.current
        val interactionSource = remember { MutableInteractionSource() }

        Modifier.combinedMouseClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            labels = labels,
            buttons = buttons,
            keyModifiers = keyModifiers,
            onDoubleClick = onDoubleClick,
            onLongPress = onLongPress,
            onClick = onClick
        )
    }
)

