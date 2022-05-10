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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

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
        if (enabled && it.isClick && keyboardModifiers(PointerKeyboardModifiers(it))) {
            onClick()
            true
        } else {
            false
        }
    }

    Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        enabled = enabled,
        role = role,
        labels = labels,
        filterScope = {
            val unconsumed = allChangesUnconsumed()
            val eligible = if (isMouse) {
                (isPress || isRelease) && relatedPointerButton == PointerButton.Primary
            } else {
                if (isPress) {
                    allChangedToDown()
                } else if (isRelease) {
                    allChangedToUp()
                } else {
                    false
                }
            }
            eligible && unconsumed && keyboardModifiers(keyModifiers)
        },
        onDoubleClick = onDoubleClick,
        onLongPress = onLongPress,
        onClick = onClick
    ).detectClickFromKey()
}

@ExperimentalFoundationApi
fun interface ClicksFilter {
    fun filter(button: PointerButton, keyModifiers: PointerKeyboardModifiers): Boolean
}

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
    clickFilter: ClicksFilter = ClicksFilter { button, _ -> button == PointerButton.Primary },
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "combinedMouseClickable"
        properties["enabled"] = enabled
        properties["clickFilter"] = clickFilter
        properties["role"] = role
        properties["labels"] = labels
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongPress"] = onLongPress
        properties["onClick"] = onClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    },
    factory = {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            labels = labels,
            filterScope = {
                isMouse &&
                    relatedPointerButton != null &&
                    clickFilter.filter(relatedPointerButton, this.keyModifiers)
            },
            onDoubleClick = onDoubleClick,
            onLongPress = onLongPress,
            onClick = onClick
        )
    }
)

@ExperimentalFoundationApi
fun Modifier.combinedMouseClickable(
    enabled: Boolean = true,
    clickFilter: ClicksFilter = ClicksFilter { button, _ -> button == PointerButton.Primary },
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "combinedMouseClickable"
        properties["enabled"] = enabled
        properties["clickFilter"] = clickFilter
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
            clickFilter = clickFilter,
            labels = labels,
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

@ExperimentalFoundationApi
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    labels: CombinedClickableLabels? = null,
    filterScope: ClickFilterScope.() -> Boolean,
    onDoubleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "combinedMixedClickable"
        properties["enabled"] = enabled
        properties["filterScope"] = filterScope
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
        val filterState = rememberUpdatedState(filterScope)

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(interactionSource) {
                val clicksHandlerScope = ClicksHandlerScope(
                    pointerInputScope = this@pointerInput,
                    interactionSource = interactionSource,
                    pressedInteraction = pressedInteraction,
                    filterState = filterState,
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
