/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.ComposeContainer
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.swing.JLayeredPane
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.hostOs

/**
 * A panel used as a main view in [ComposeWindow] and [ComposeDialog].
 */
internal class ComposeWindowPanel(
    private val window: Window,
    private val isUndecorated: () -> Boolean,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : JLayeredPane() {
    private var isDisposed = false

    // AWT can leak JFrame in some cases
    // (see https://github.com/JetBrains/compose-jb/issues/1688),
    // so we nullify bridge on dispose, to prevent keeping
    // big objects in memory (like the whole LayoutNode tree of the window)
    private var _composeContainer: ComposeContainer? = ComposeContainer(
        this,
        skiaLayerAnalytics,
        layoutDirection = layoutDirectionFor(window),
        createSkiaLayerComponent = ::WindowSkiaLayerComponent
    )
    private val composeContainer
        get() = requireNotNull(_composeContainer) {
            "ComposeContainer is disposed"
        }

    val windowAccessible get() = composeContainer.accessible
    val windowContext get() = composeContainer.windowContext
    var rootForTestListener by composeContainer::rootForTestListener
    var fullscreen by composeContainer::fullscreen
    var compositionLocalContext by composeContainer::compositionLocalContext
    var exceptionHandler by composeContainer::exceptionHandler
    val windowHandle get() = composeContainer.windowHandle
    val renderApi get() = composeContainer.renderApi

    var isWindowTransparent: Boolean = false
        set(value) {
            if (field != value) {
                check(isUndecorated()) { "Transparent window should be undecorated!" }
                check(!window.isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                field = value
                composeContainer.onChangeWindowTransparency(value)

                /*
                 * Windows makes clicks on transparent pixels fall through, but it doesn't work
                 * with GPU accelerated rendering since this check requires having access to pixels from CPU.
                 *
                 * JVM doesn't allow override this behaviour with low-level windows methods, so hack this in this way.
                 * Based on tests, it doesn't affect resulting pixel color.
                 *
                 * Note: Do not set isOpaque = false for this container
                 */
                if (value && hostOs == OS.Windows) {
                    background = Color(0, 0, 0, 1)
                    isOpaque = true
                } else {
                    background = null
                    isOpaque = false
                }

                window.background = if (value && !skikoTransparentWindowHack) Color(0, 0, 0, 0) else null
            }
        }

    /**
     * There is a hack inside skiko OpenGL and Software redrawers for Windows that makes current
     * window transparent without setting `background` to JDK's window. It's done by getting native
     * component parent and calling `DwmEnableBlurBehindWindow`.
     *
     * FIXME: Make OpenGL work inside transparent window (background == Color(0, 0, 0, 0)) without this hack.
     *
     * See `enableTransparentWindow` (skiko/src/awtMain/cpp/windows/window_util.cc)
     */
    private val skikoTransparentWindowHack: Boolean
        get() = hostOs == OS.Windows && renderApi != GraphicsApi.DIRECT3D

    init {
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?) = null
            override fun getComponentBefore(aContainer: Container?, aComponent: Component?) = null
            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        isFocusCycleRoot = true
        setContent {}
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        composeContainer.setBounds(0, 0, width, height)
        super.setBounds(x, y, width, height)
    }

    override fun add(component: Component): Component {
        composeContainer.addToComponentLayer(component)
        return component
    }

    override fun remove(component: Component) {
        composeContainer.removeFromComponentLayer(component)
        super.remove(component)
    }

    override fun getPreferredSize(): Dimension? = if (isPreferredSizeSet) {
        super.getPreferredSize()
    } else {
        composeContainer.preferredSize
    }

    override fun addNotify() {
        super.addNotify()
        _composeContainer?.contentComponent?.requestFocus()
    }

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        composeContainer.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        composeContainer.setContent {
            CompositionLocalProvider(
                LocalWindow provides window
            ) {
                WindowContentLayout(modifier, content)
            }
        }
    }

    fun dispose() {
        if (isDisposed) {
            return
        }
        _composeContainer?.dispose()
        _composeContainer = null
        isDisposed = true
    }

    fun onChangeLayoutDirection(component: Component) {
        composeContainer.onChangeLayoutDirection(component)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        composeContainer.onRenderApiChanged(action)
    }

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) =
        composeContainer.addMouseListener(listener)

    override fun removeMouseListener(listener: MouseListener) =
        composeContainer.removeMouseListener(listener)

    override fun addMouseMotionListener(listener: MouseMotionListener) =
        composeContainer.addMouseMotionListener(listener)

    override fun removeMouseMotionListener(listener: MouseMotionListener) =
        composeContainer.removeMouseMotionListener(listener)

    override fun addMouseWheelListener(listener: MouseWheelListener) =
        composeContainer.addMouseWheelListener(listener)

    override fun removeMouseWheelListener(listener: MouseWheelListener) =
        composeContainer.removeMouseWheelListener(listener)
}
