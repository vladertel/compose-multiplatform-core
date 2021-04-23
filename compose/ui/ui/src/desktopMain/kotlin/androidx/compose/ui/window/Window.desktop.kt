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

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.ComposeWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.setPositionSafely
import androidx.compose.ui.util.setSizeSafely
import androidx.compose.ui.util.setUndecoratedSafely
import java.awt.Image
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JMenuBar

// TODO(demin): support focus management
/**
 * Composes platform window in the current composition. When Window enters the composition,
 * a new platform window will be created and receives the focus. When Window leaves the
 * composition, window will be disposed and closed.
 * Window will also be disposed and closed when [WindowState.isOpen] will become `false`.
 *
 * Initial size of the window is controlled by [WindowState.size].
 * Initial position of the window is controlled by [WindowState.position] and [initialAlignment].
 *
 * @param state The state object to be used to control or observe the window's state
 * When size/position/status is changed by the user, state will be updated.
 * When size/position/status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.isMaximized], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is [WindowPosition.isInitial], then after the first show on the screen
 * [WindowState.position] will be set to the absolute values.
 * @param title Title in the titlebar of the window
 * @param icon Icon in the titlebar of the window (for platforms which support this)
 * @param resizable Can window be resized by the user (application still can resize the window
 * changing [state])
 * @param enabled Can window react to input events
 * @param focusable Can window receive focus
 * @param alwaysOnTop Should window always be on top of another windows
 * @param initialAlignment if not null and If [WindowState.position] is [WindowPosition.isInitial],
 * window will be aligned when it will be shown on the screen
 * @param onCloseRequest callback that will be called when the user closes the window. By default
 * it changes implicit [WindowState.isOpen] state
 * @param content content of the window
 *
 * This API is experimental and will eventually replace [AppWindow] / [AppManager]
 */
@ExperimentalComposeUiApi
@Composable
fun Window(
    state: WindowState = rememberWindowState(),
    title: String = "Untitled",
    // TODO(demin): can we replace this by icon: Painter? What to do with different densities?
    icon: Image? = null,
    undecorated: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    initialAlignment: Alignment? = null,
//    TODO(demin): implement adaptive window size. We should only add this property here and leave
//     WindowState.size as it is. If this property is true then we will ignore the size in
//     WindowState and reset it on the attach.
//    wrapContent: Boolean = false,
    onCloseRequest: () -> Unit = { state.isOpen = false },
    content: @Composable WindowScope.() -> Unit
) {
    if (state.isOpen) {
        val currentState by rememberUpdatedState(state)
        val currentTitle by rememberUpdatedState(title)
        val currentIcon by rememberUpdatedState(icon)
        val currentUndecorated by rememberUpdatedState(undecorated)
        val currentResizable by rememberUpdatedState(resizable)
        val currentEnabled by rememberUpdatedState(enabled)
        val currentFocusable by rememberUpdatedState(focusable)
        val currentAlwaysOnTop by rememberUpdatedState(alwaysOnTop)
        val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

        val updater = remember(::ComponentUpdater)

        Window(
            visible = currentState.isVisible,
            create = {
                ComposeWindow().apply {
                    // close state is controlled by WindowState.isOpen
                    defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent) {
                            currentOnCloseRequest()
                        }
                    })
                    addWindowStateListener {
                        currentState.isMaximized = isMaximized
                        currentState.isMinimized = isMinimized
                    }
                    addComponentListener(object : ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent) {
                            currentState.isFullscreen = isFullscreen
                            currentState.size = WindowSize(width.dp, height.dp)
                        }

                        override fun componentMoved(e: ComponentEvent) {
                            currentState.position = WindowPosition(x.dp, y.dp)
                        }
                    })
                }
            },
            dispose = ComposeWindow::dispose,
            update = { window ->
                updater.update {
                    set(currentTitle, window::setTitle)
                    set(currentIcon, window::setIconImage)
                    set(currentUndecorated, window::setUndecoratedSafely)
                    set(currentResizable, window::setResizable)
                    set(currentEnabled, window::setEnabled)
                    set(currentFocusable, window::setFocusable)
                    set(currentAlwaysOnTop, window::setAlwaysOnTop)
                    set(state.size, window::setSizeSafely)
                    set(state.position) {
                        window.setPositionSafely(it, initialAlignment)
                    }
                    set(state.isFullscreen, window::isFullscreen::set)
                    set(state.isMaximized, window::isMaximized::set)
                    set(state.isMinimized, window::isMinimized::set)
                }
            },
            content = content
        )
    }
}

/**
 * Compose [ComposeWindow] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeWindow] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once Window leaves the composition, [dispose] will be called to free resources that
 * obtained by the [ComposeWindow].
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeWindow] properties depending on state.
 * When state changes, the block will be reexecuted to set the new properties.
 * Note the block will also be ran once right after the [create] block completes.
 *
 * Window is needed for creating window's that still can't be created with
 * the default Compose function [androidx.compose.ui.window.Window]
 *
 * This API is experimental and will eventually replace [AppWindow] / [AppManager].
 *
 * @param visible Is [ComposeWindow] visible to user.
 * If `false`:
 * - internal state of [ComposeWindow] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * the composition.
 * @param create The block creating the [ComposeWindow] to be composed.
 * @param dispose The block to dispose [ComposeWindow] and free native resources.
 * Usually it is simple `ComposeWindow::dispose`
 * @param update The callback to be invoked after the layout is inflated.
 * @param content Composable content of the creating window.
 */
@ExperimentalComposeUiApi
@Composable
fun Window(
    visible: Boolean = true,
    create: () -> ComposeWindow,
    dispose: (ComposeWindow) -> Unit,
    update: (ComposeWindow) -> Unit = {},
    content: @Composable WindowScope.() -> Unit
) {
    val composition = rememberCompositionContext()
    AwtWindow(
        visible = visible,
        create = {
            create().apply {
                val scope = WindowScope(this)
                setContent(composition) {
                    scope.content()
                }
            }
        },
        dispose = dispose,
        update = update
    )
}

/**
 * Receiver scope which is used by [androidx.compose.ui.window.Window].
 */
class WindowScope(
    /**
     * [ComposeWindow] that was created inside [androidx.compose.ui.window.Window].
     */
    val window: ComposeWindow
)

/**
 * Composes menu bar on the top of the window
 *
 * @param content content of the menu bar (list of menus)
 */
@Composable
fun WindowScope.MenuBar(content: @Composable MenuBarScope.() -> Unit) {
    val parentComposition = rememberCompositionContext()

    DisposableEffect(Unit) {
        val menu = JMenuBar()
        val composition = menu.setContent(parentComposition, content)
        window.jMenuBar = menu
        composition to menu

        onDispose {
            window.jMenuBar = null
            composition.dispose()
        }
    }
}