/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window.window

import androidx.compose.desktop.ComposeWindow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.isLinux
import androidx.compose.ui.window.isWindows
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.math.max

// Note that on Linux some tests are flaky. Swing event listener's on Linux has non-deterministic
// nature. To avoid flaky'ness we use delays.
// It is not a good solution, but it works.

// TODO(demin): figure out how can we fix flaky tests on Linux
// TODO(demin): fix fullscreen tests on macOs

@OptIn(ExperimentalComposeUiApi::class)
class WindowStateTest {
    @Test
    fun `manually close window`() = runApplicationTest {
        var window: ComposeWindow? = null
        val state = WindowState(size = WindowSize(200.dp, 200.dp))

        launchApplication {
            Window(state = state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()
        assertThat(state.isOpen).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
        assertThat(state.isOpen).isFalse()
    }

    @Test
    fun `programmatically close window`() = runApplicationTest {
        var window: ComposeWindow? = null
        val state = WindowState(size = WindowSize(200.dp, 200.dp))

        launchApplication {
            Window(state = state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        state.isOpen = false
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
    }

    @Test
    fun `programmatically open and close nested window`() = runApplicationTest {
        var parentWindow: ComposeWindow? = null
        var childWindow: ComposeWindow? = null
        val parentState = WindowState(size = WindowSize(400.dp, 400.dp))
        val childState = WindowState(isOpen = false, size = WindowSize(200.dp, 200.dp))

        launchApplication {
            Window(parentState) {
                parentWindow = this.window

                Window(childState) {
                    childWindow = this.window
                }
            }
        }

        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()

        childState.isOpen = true
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isTrue()

        childState.isOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isFalse()

        parentState.isOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isFalse()
    }

    @Test
    fun `set size and position before show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = WindowSize(200.dp, 200.dp),
            position = WindowPosition(242.dp, 242.dp)
        )

        var window: ComposeWindow? = null

        launchApplication {
            Window(state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(200, 200))
        assertThat(window?.location).isEqualTo(Point(242, 242))

        state.isOpen = false
    }

    @Test
    fun `change position after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = WindowSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(state) {
                window = this.window
            }
        }

        awaitIdle()

        state.position = WindowPosition(242.dp, (242).dp)
        awaitIdle()
        assertThat(window?.location).isEqualTo(Point(242, 242))

        state.isOpen = false
    }

    @Test
    fun `change size after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = WindowSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(state) {
                window = this.window
            }
        }

        awaitIdle()

        state.size = WindowSize(250.dp, 200.dp)
        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(250, 200))

        state.isOpen = false
    }

    @Test
    fun `center window`() = runApplicationTest {
        fun Rectangle.center() = Point(x + width / 2, y + height / 2)
        fun JFrame.center() = bounds.center()
        fun JFrame.screenCenter() = graphicsConfiguration.bounds.center()
        infix fun Point.maxDistance(other: Point) = max(abs(x - other.x), abs(y - other.y))

        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window!!.center() maxDistance window!!.screenCenter() < 250)

        state.isOpen = false
    }

    @Test
    fun `remember position after reattach`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null
        var isWindow1 by mutableStateOf(true)

        launchApplication {
            if (isWindow1) {
                Window(state, initialAlignment = Alignment.Center) {
                    window1 = this.window
                }
            } else {
                Window(state, initialAlignment = Alignment.Center) {
                    window2 = this.window
                }
            }
        }

        awaitIdle()

        state.position = WindowPosition(242.dp, 242.dp)
        awaitIdle()
        assertThat(window1?.location == Point(242, 242))

        isWindow1 = false
        awaitIdle()
        assertThat(window2?.location == Point(242, 242))

        state.isOpen = false
    }

    @Test
    fun `state position shouldn't be initial after attach`() = runApplicationTest {
        val state = WindowState(size = WindowSize(200.dp, 200.dp))

        launchApplication {
            Window(state) {
            }
        }

        awaitIdle()
        assertThat(state.position.isInitial)

        state.isOpen = false
    }

    @Test
    fun `enter fullscreen`() = runApplicationTest(useDelay = isLinux) {
        // TODO(demin): fix macOs. We disabled it because it is not deterministic.
        //  If we set in skiko SkiaLayer.setFullscreen(true) then isFullscreen still returns false
        assumeTrue(isWindows || isLinux)

        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()

        // TODO(demin): we use await instead of awaitIdle, because isFullscreen on macOs doesn't
        //  apply immediately. It is because in skiko we dispatch setFullscrenn into AppKit thread.
        //  We should fix SkiaLayaer.isFullscreen in skiko. It should be immediately be updated
        //  after SkiaLayer.setFullscreen
        state.isFullscreen = true
        awaitIdle()
        assertThat(window?.isFullscreen).isTrue()

        state.isFullscreen = false
        awaitIdle()
        assertThat(window?.isFullscreen).isFalse()

        state.isOpen = false
    }

    @Test
    fun maximize() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMaximized = true
        awaitIdle()
        assertThat(window?.isMaximized).isTrue()

        state.isMaximized = false
        awaitIdle()
        assertThat(window?.isMaximized).isFalse()

        state.isOpen = false
    }

    @Test
    fun minimize() = runApplicationTest {
        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        awaitIdle()
        assertThat(window?.isMinimized).isTrue()

        state.isMinimized = false
        awaitIdle()
        assertThat(window?.isMinimized).isFalse()

        state.isOpen = false
    }

    @Test
    fun `maximize, minimize and enter fullscreen`() = runApplicationTest {
        // TODO(demin): fix fullscreen on macOs
        assumeTrue(isWindows || isLinux)

        val state = WindowState(size = WindowSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        state.isMaximized = true
        state.isFullscreen = true
        awaitIdle()
        assertThat(window?.isMinimized).isTrue()
        assertThat(window?.isMaximized).isTrue()
        assertThat(window?.isFullscreen).isTrue()

        state.isOpen = false
    }

    @Test
    fun `restore size and position after maximize`() = runApplicationTest {
        // Swing/macOs can't re-change isMaximized in a deterministic way:
//        fun main() = runBlocking(Dispatchers.Swing) {
//            val window = ComposeWindow()
//            window.size = Dimension(200, 200)
//            window.isVisible = true
//            window.isMaximized = true
//            delay(100)
//            window.isMaximized = false  // we cannot do that on macOs (window is still animating)
//            delay(1000)
//            println(window.isMaximized) // prints true
//        }
//        Swing/Linux has animations and sometimes adds an offset to the size/position
        assumeTrue(isWindows)

        val state = WindowState(
            size = WindowSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.isMaximized = true
        awaitIdle()
        assertThat(window?.isMaximized).isTrue()
        assertThat(window?.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window?.location).isNotEqualTo(Point(196, 257))

        state.isMaximized = false
        awaitIdle()
        assertThat(window?.isMaximized).isFalse()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.isOpen = false
    }

    @Test
    fun `restore size and position after fullscreen`() = runApplicationTest {
//        Swing/Linux has animations and sometimes adds an offset to the size/position
        assumeTrue(isWindows)

        val state = WindowState(
            size = WindowSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.isFullscreen = true
        awaitIdle()
        assertThat(window?.isFullscreen).isTrue()
        assertThat(window?.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window?.location).isNotEqualTo(Point(196, 257))

        state.isFullscreen = false
        awaitIdle()
        assertThat(window?.isFullscreen).isFalse()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.isOpen = false
    }

    @Test
    fun `maximize window before show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = WindowSize(200.dp, 200.dp), isMaximized = true)
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isMaximized).isTrue()

        state.isOpen = false
    }

    @Test
    fun `minimize window before show`() = runApplicationTest {
        // Linux/macos doesn't support this:
//        fun main() = runBlocking(Dispatchers.Swing) {
//            val window = ComposeWindow()
//            window.size = Dimension(200, 200)
//            window.isMinimized = true
//            window.isVisible = true
//            delay(2000)
//            println(window.isMinimized) // prints false
//        }
        // TODO(demin): can we minimize after window.isVisible?
        assumeTrue(isWindows)

        val state = WindowState(size = WindowSize(200.dp, 200.dp), isMinimized = true)
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isMinimized).isTrue()

        state.isOpen = false
    }

    @Test
    fun `enter fullscreen before show`() = runApplicationTest {
        // TODO(demin): probably we have a bug in skiko (we can't change fullscreen on macOs before
        //  showing the window)
        assumeTrue(isLinux || isWindows)

        val state = WindowState(size = WindowSize(200.dp, 200.dp), isFullscreen = true)
        var window: ComposeWindow? = null

        launchApplication {
            Window(state, initialAlignment = Alignment.Center) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isFullscreen).isTrue()

        state.isOpen = false
    }

    @Test
    fun `emit non-opened window`() = runApplicationTest {
        val state = WindowState(isOpen = false)

        launchApplication {
            Window(state = state) {}
        }
    }

    @Test
    fun `save state`() = runApplicationTest {
        val initialState = WindowState()
        val newState = WindowState(
            isOpen = false,
            size = WindowSize(42.dp, 42.dp),
            position = WindowPosition(3.dp, 3.dp),
            isFullscreen = true,
            isMaximized = true,
            isMinimized = true,
        )

        var isOpen by mutableStateOf(true)
        var index by mutableStateOf(0)
        val states = mutableListOf<WindowState>()

        launchApplication {
            val saveableStateHolder = rememberSaveableStateHolder()
            saveableStateHolder.SaveableStateProvider(index) {
                val state = rememberWindowState()

                LaunchedEffect(Unit) {
                    state.isOpen = newState.isOpen
                    state.isFullscreen = newState.isFullscreen
                    state.isMaximized = newState.isMaximized
                    state.isMinimized = newState.isMinimized
                    state.size = newState.size
                    state.position = newState.position
                    states.add(state)
                }
            }

            // TODO(demin): don't end application if there are no windows but still pending tasks
            if (isOpen) {
                Window {}
            }
        }

        awaitIdle()
        assertThat(states.size == 1)

        index = 1
        awaitIdle()
        assertThat(states.size == 2)

        index = 0
        awaitIdle()
        assertThat(states.size == 3)

        assertThat(states[0].isOpen == initialState.isOpen)
        assertThat(states[0].isFullscreen == initialState.isFullscreen)
        assertThat(states[0].isMaximized == initialState.isMaximized)
        assertThat(states[0].isMinimized == initialState.isMinimized)
        assertThat(states[0].size == initialState.size)
        assertThat(states[0].position == initialState.position)
        assertThat(states[2].isOpen == newState.isOpen)
        assertThat(states[2].isFullscreen == newState.isFullscreen)
        assertThat(states[2].isMaximized == newState.isMaximized)
        assertThat(states[2].isMinimized == newState.isMinimized)
        assertThat(states[2].size == newState.size)
        assertThat(states[2].position == newState.position)

        isOpen = false
    }
}