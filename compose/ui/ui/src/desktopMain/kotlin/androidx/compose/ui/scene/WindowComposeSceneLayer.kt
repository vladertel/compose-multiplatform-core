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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.awt.toAwtColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerAdapter
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.scaledSize
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JDialog
import javax.swing.JLayeredPane
import javax.swing.RootPaneContainer
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class WindowComposeSceneLayer(
    private val composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
) : DesktopComposeSceneLayer() {
    private val windowContext = PlatformWindowContext().also {
        it.isWindowTransparent = true
    }
    private val window get() = requireNotNull(composeContainer.window)
    private val windowContent: Component
        get() {
            var content: Component = window
            if (content is RootPaneContainer) {
                content = content.contentPane
            }
            return content
        }

    private val dialog = JDialog(
        window,
    ).also {
        it.isAlwaysOnTop = true
        it.isUndecorated = true
        it.background = Color.Transparent.toAwtColor()
    }
    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            _mediator?.onComponentAttached()
        }
    }.also {
        it.layout = null
        it.isOpaque = false

        dialog.contentPane = it
    }

    private var _mediator: ComposeSceneMediator? = null

    override var density: Density = density
        set(value) {
            field = value
            // TODO: Pass it to mediator/scene
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            field = value
            // TODO: Pass it to mediator/scene
        }

    override var focusable: Boolean = focusable
        set(value) {
            field = value
            // TODO: Pass it to mediator/scene
        }

    override var bounds: IntRect = IntRect.Zero
        set(value) {
            field = value

            val scaledRectangle = value.toAwtRectangle(density)
            dialog.location = getDialogLocation(scaledRectangle.x, scaledRectangle.y)
            dialog.setSize(scaledRectangle.width, scaledRectangle.height)
            _mediator?.contentOffset = bounds.topLeft.toOffset()
            _mediator?.contentBounds = Rectangle(0, 0, scaledRectangle.width, scaledRectangle.height)
        }

    override var scrimColor: Color? = null
        set(value) {
            field = value
            // TODO: Draw scrim in the main window
        }

    init {
        _mediator = ComposeSceneMediator(
            container = container,
            windowContext = windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            coroutineContext = compositionContext.effectCoroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.transparency = true
            it.containerSize = windowContent.scaledSize
        }
        dialog.location = getDialogLocation(0, 0)
        dialog.size = windowContent.size
        dialog.isVisible = true
        composeContainer.attachLayer(this)
    }

    override fun close() {
        composeContainer.detachLayer(this)
        _mediator?.dispose()
        dialog.dispose()
    }

    override fun setContent(content: @Composable () -> Unit) {
        _mediator?.setContent(content)
    }

    override fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        _mediator?.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent ?: { false },
            onKeyEvent = onKeyEvent ?: { false }
        )
    }

    override fun setOutsidePointerEventListener(onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)?) {
        // TODO
    }

    override fun onChangeWindowBounds() {
        val scaledRectangle = bounds.toAwtRectangle(density)
        dialog.location = getDialogLocation(scaledRectangle.x, scaledRectangle.y)

        // Update compose constrains based on main window size
        _mediator?.containerSize = windowContent.scaledSize
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return SkiaLayerAdapter(mediator, windowContext, skiaLayerAnalytics)
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        val layoutDirection = layoutDirectionFor(container)
        return SingleLayerComposeScene(
            coroutineContext = mediator.coroutineContext,
            density = density,
            invalidate = mediator::onComposeSceneInvalidate,
            layoutDirection = layoutDirection,
            composeSceneContext = composeContainer.createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
        )
    }

    private fun getDialogLocation(x: Int, y: Int): Point {
        val locationOnScreen = windowContent.locationOnScreen
        return Point(
            locationOnScreen.x + x,
            locationOnScreen.y + y
        )
    }
}

private fun IntRect.toAwtRectangle(density: Density): Rectangle {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    val width = right - left
    val height = bottom - top
    return Rectangle(
        left, top, width, height
    )
}
