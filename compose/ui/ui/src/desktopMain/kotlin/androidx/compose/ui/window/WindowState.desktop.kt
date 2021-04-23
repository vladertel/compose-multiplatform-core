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

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param isOpen the initial value for [WindowState.isOpen]
 * @param isVisible the initial value for [WindowState.isVisible]
 * @param isFullscreen the initial value for [WindowState.isFullscreen]
 * @param isMaximized the initial value for [WindowState.isMaximized]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
@Composable
fun rememberWindowState(
    isOpen: Boolean = true,
    isVisible: Boolean = true,
    isFullscreen: Boolean = false,
    isMaximized: Boolean = false,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.Initial,
    size: WindowSize = WindowSize(800.dp, 600.dp),
): WindowState = rememberSaveable(saver = WindowState.Saver) {
    WindowState(
        isOpen,
        isVisible,
        isFullscreen,
        isMaximized,
        isMinimized,
        position,
        size
    )
}

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * In most cases, this will be created via [rememberWindowState].
 *
 * @param isOpen the initial value for [WindowState.isOpen]
 * @param isVisible the initial value for [WindowState.isVisible]
 * @param isFullscreen the initial value for [WindowState.isFullscreen]
 * @param isMaximized the initial value for [WindowState.isMaximized]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
class WindowState(
    isOpen: Boolean = true,
    isVisible: Boolean = true,
    isFullscreen: Boolean = false,
    isMaximized: Boolean = false,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.Initial,
    size: WindowSize = WindowSize(800.dp, 600.dp)
) {
    /**
     * `true` if the window is open and shown to user.
     * This state can be controlled by the user (when the user closes the window isOpen
     * will become `false`)
     * or by  application:
     * ```
     * state.isOpen = false // close the window, free resources, forget the internal state
     * ```
     *
     * Difference between isOpen and isVisible:
     * - isOpen disposes the window (if it is the last window application will end) and forgets the
     * internal state
     * - isVisible doesn't dispose the window (application doesn't end) and doesn't forget the
     * internal state
     */
    var isOpen by mutableStateOf(isOpen)

    /**
     * `true` if the window is visible to user. `false` if it isn't visible, but still has
     * preserved internal state. Next time the window will be visible again,
     * the internal state will be restored.
     *
     * Difference between isOpen and isVisible:
     * - isOpen disposes the window (if it is the last window application will end) and forgets the
     * internal state
     * - isVisible doesn't dispose the window (application doesn't end) and doesn't forget the
     * internal state
     */
    var isVisible by mutableStateOf(isVisible)

    /**
     * `true` if the window is in fullscreen mode
     */
    var isFullscreen by mutableStateOf(isFullscreen)

    /**
     * `true` if the window is maximized
     */
    var isMaximized by mutableStateOf(isMaximized)

    /**
     * `true` if the window is minimized
     */
    var isMinimized by mutableStateOf(isMinimized)

    /**
     * Current position of the window. If [position] is [WindowPosition.isInitial] then after
     * [WindowState] will be attached to [Window] and [Window] will be shown to the user,
     * [position] will be assigned to the absolute values.
     *
     * Note that if window is in fullscreen mode, or maximized, it represents the real position
     * of the window (not the position that will be restored after we reset the state)
     */
    var position by mutableStateOf(position)

    /**
     * Current size of the window.
     * Note that if window is in fullscreen mode, or maximized, it represents the full size
     * of the window (not the size that will be restored after we reset the state)
     */
    var size by mutableStateOf(size)

    companion object {
        /**
         * The default [Saver] implementation for [WindowState].
         */
        val Saver = listSaver<WindowState, Any>(
            save = {
                listOf(
                    it.isOpen,
                    it.isVisible,
                    it.isFullscreen,
                    it.isMaximized,
                    it.isMinimized,
                    it.position.x.value,
                    it.position.y.value,
                    it.size.width.value,
                    it.size.height.value,
                )
            },
            restore = { state ->
                WindowState(
                    isOpen = state[0] as Boolean,
                    isVisible = state[1] as Boolean,
                    isFullscreen = state[2] as Boolean,
                    isMaximized = state[3] as Boolean,
                    isMinimized = state[4] as Boolean,
                    position = WindowPosition((state[5] as Float).dp, (state[6] as Float).dp),
                    size = WindowSize((state[7] as Float).dp, (state[8] as Float).dp),
                )
            }
        )
    }
}