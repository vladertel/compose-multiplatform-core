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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
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
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@ExperimentalFoundationApi
fun Modifier.onPrimaryCombinedClickable(
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
    keyboardModifiers: (PointerKeyboardModifiers) -> Boolean = { _ -> true },
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {

    fun Modifier.detectClickFromKey() = this.onKeyEvent {
        if (enabled && it.isClick && keyboardModifiers(it.toPointerKeyboardModifiers())) {
            onClick()
            true
        } else {
            false
        }
    }

    Modifier.customCombinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        enabled = enabled,
        role = role,
        labels = labels,
        pressFilter = {
            val isMouse = it.changes.fastAll { it.type == PointerType.Mouse }
            val changesUnconsumed = it.changes.fastAll { !it.isConsumed }
            val eligible = if (isMouse) {
                it.buttons.isPrimaryPressed
            } else {
                it.changes.fastAll { it.changedToDown() }
            }
            eligible && changesUnconsumed
        },
        releaseFilter = {
            val isMouse = it.changes.fastAll { it.type == PointerType.Mouse }
            val changesUnconsumed = it.changes.fastAll { !it.isConsumed }
            val eligible = if (isMouse) {
                !it.buttons.isPrimaryPressed
            } else {
                it.changes.fastAll { it.changedToUp() }
            }
            eligible && changesUnconsumed
        },
        keyModifiers = keyboardModifiers,
        onDoubleClick = onDoubleClick,
        onLongPress = onLongPress,
        onClick = onClick
    ).detectClickFromKey()
}

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
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
        Modifier.customCombinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            labels = labels,
            pressFilter = { buttons(it.buttons) },
            releaseFilter = { !buttons(it.buttons) },
            keyModifiers = keyModifiers,
            onDoubleClick = onDoubleClick,
            onLongPress = onLongPress,
            onClick = onClick
        )
    }
)

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    enabled: Boolean = true,
    buttons: (PointerButtons) -> Boolean = { it.isPrimaryPressed },
    keyModifiers: (PointerKeyboardModifiers) -> Boolean = { true },
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
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

data class CombinedClickableLabels(
    val onDoubleClickLabel: String? = null,
    val onLongPressLabel: String? = null,
    val onClickLabel: String? = null
)

private class ClicksHandlerScope(
    pointerInputScope: PointerInputScope,
    val interactionSource: MutableInteractionSource,
    val pressedInteraction: MutableState<PressInteraction.Press?>,
    val filterForPressEvent: State<(PointerEvent) -> Boolean>,
    val releaseFilter: State<(PointerEvent) -> Boolean>,
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

    suspend fun awaitEvents() {
        coroutineScope {
            awaitPointerEventScope {
                pressScope.reset()

                // wait until a button is unpressed if needed
                while (filterForPressEvent.value(currentEvent)) {
                    awaitPointerEvent()
                }

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
        return awaitPress(filterForPressEvent.value, filterKeyboardModifiers.value)
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

        // check if already released
        if (releaseFilter.value(currentEvent)) return null

        while (event == null || changes.isEmpty()) {
            event = awaitPointerEvent()

            val cancelled = event.changes.fastAny {
                it.isOutOfBounds(size, Size.Zero)
            } || !filterKeyboardModifiers.value(event.keyboardModifiers)

            if (cancelled) return null

            event = event.takeIf { releaseFilter.value(it) }

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
    filterPressEvent: (PointerEvent) -> Boolean,
    filterKeyboardModifiers: (PointerKeyboardModifiers) -> Boolean = { _ -> true },
    requireUnconsumed: Boolean = true
): PointerEvent {
    var event: PointerEvent? = null
    var changes: List<PointerInputChange> = emptyList()

    while (event == null || changes.isEmpty()) {
        val previousEvent = currentEvent

        event = awaitPointerEvent().takeIf {
            !filterPressEvent(previousEvent) &&
                filterPressEvent(it) &&
                filterKeyboardModifiers(it.keyboardModifiers)
        }
        changes = event?.changes?.takeIf {
            !requireUnconsumed || it.fastAll { !it.isConsumed }
        } ?: emptyList()
    }

    return event
}

@ExperimentalFoundationApi
internal fun Modifier.customCombinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
    pressFilter: (PointerEvent) -> Boolean,
    releaseFilter: (PointerEvent) -> Boolean,
    keyModifiers: (PointerKeyboardModifiers) -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "combinedMixedClickable"
        properties["enabled"] = enabled
        properties["pressFilter"] = pressFilter
        properties["releaseFilter"] = releaseFilter
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
        val pressFilterState = rememberUpdatedState(pressFilter)
        val releaseFilterState = rememberUpdatedState(releaseFilter)
        val modifiersFilterState = rememberUpdatedState(keyModifiers)

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(interactionSource) {
                val clicksHandlerScope = ClicksHandlerScope(
                    pointerInputScope = this@pointerInput,
                    interactionSource = interactionSource,
                    pressedInteraction = pressedInteraction,
                    filterForPressEvent = pressFilterState,
                    releaseFilter = releaseFilterState,
                    filterKeyboardModifiers = modifiersFilterState,
                    onDoubleClick = on2xClickState,
                    onLongPress = onLongClickState,
                    onClick = onClickState
                )

                while (currentCoroutineContext().isActive) {
                    clicksHandlerScope.awaitEvents()
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
