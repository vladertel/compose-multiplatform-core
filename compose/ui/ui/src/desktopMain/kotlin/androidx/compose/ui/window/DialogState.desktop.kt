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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState.Companion.Saver

/**
 * Creates a [DialogState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param isOpen the initial value for [DialogState.isOpen]
 * @param position the initial value for [DialogState.position]
 * @param size the initial value for [DialogState.size]
 */
@Composable
fun rememberDialogState(
    isOpen: Boolean = true,
    isVisible: Boolean = true,
    position: WindowPosition = WindowPosition.Initial,
    size: WindowSize = WindowSize(800.dp, 600.dp),
): DialogState = rememberSaveable(saver = DialogState.Saver) {
    DialogState(
        isOpen,
        isVisible,
        position,
        size
    )
}

/**
 * A state object that can be hoisted to control and observe dialog attributes
 * (size/position).
 *
 * In most cases, this will be created via [rememberDialogState].
 *
 * @param isOpen the initial value for [DialogState.isOpen]
 * @param position the initial value for [DialogState.position]
 * @param size the initial value for [DialogState.size]
 */
class DialogState(
    isOpen: Boolean = true,
    isVisible: Boolean = true,
    position: WindowPosition = WindowPosition.Initial,
    size: WindowSize = WindowSize(800.dp, 600.dp)
) {
    /**
     * `true` if the dialog is open and shown to user.
     * This state can be controlled by the user (when the user closes the dialog isOpen
     * will become `false`)
     * or by  application:
     * ```
     * state.isOpen = false // close the dialog, free resources, forget the internal state
     * ```
     *
     * Difference between isOpen and isVisible:
     * - isOpen disposes the dialog (if it is the last dialog/window application will end) and
     * forgets the internal state
     * - isVisible doesn't dispose the dialog (application doesn't end) and doesn't forget the
     * internal state
     */
    var isOpen by mutableStateOf(isOpen)

    /**
     * `true` if the window is visible to user. `false` if it isn't visible, but still has
     * preserved internal state. Next time the window will be visible again,
     * the internal state will be restored.
     *
     * Difference between isOpen and isVisible:
     * - isOpen disposes the dialog (if it is the last dialog/window application will end) and
     * forgets the internal state
     * - isVisible doesn't dispose the dialog (application doesn't end) and doesn't forget the
     * internal state
     */
    var isVisible by mutableStateOf(isVisible)

    /**
     * Current position of the dialog. If [position] is [WindowPosition.isInitial] then after
     * [DialogState] will be attached to [Dialog] and [Dialog] will be shown to the user,
     * [position] will be assigned to the absolute values.
     */
    var position by mutableStateOf(position)

    /**
     * Current size of the dialog.
     */
    var size by mutableStateOf(size)

    companion object {
        /**
         * The default [Saver] implementation for [DialogState].
         */
        val Saver = listSaver<DialogState, Any>(
            save = {
                listOf(
                    it.isOpen,
                    it.isVisible,
                    it.position.x.value,
                    it.position.y.value,
                    it.size.width.value,
                    it.size.height.value,
                )
            },
            restore = { state ->
                DialogState(
                    isOpen = state[0] as Boolean,
                    isVisible = state[1] as Boolean,
                    position = WindowPosition((state[2] as Float).dp, (state[3] as Float).dp),
                    size = WindowSize((state[4] as Float).dp, (state[5] as Float).dp),
                )
            }
        )
    }
}