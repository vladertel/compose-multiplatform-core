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
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerAdapter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JFrame
import javax.swing.JLayeredPane
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class SwingComposeSceneLayer(
    private val composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
) : DesktopComposeSceneLayer(), MouseListener {
    private val window get() = requireNotNull(composeContainer.window)
    private val layersContainer get() = requireNotNull(composeContainer.layersContainer)

    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            _mediator?.onComponentAttached()
            _mediator?.contentBounds = this@SwingComposeSceneLayer.bounds.toAwtRectangle(density)
        }

        override fun paint(g: Graphics) {
            g.color = background
            g.fillRect(0,0, width, height)

            // Draw content after background
            super.paint(g)
        }
    }.also {
        it.layout = null
        it.isOpaque = false
        it.background = Color.Transparent.toAwtColor()
        it.size = Dimension(window.width, window.height)
        it.addMouseListener(this)

        // TODO: Currently it works only with offscreen rendering
        layersContainer.add(it, JLayeredPane.POPUP_LAYER, 0)
    }
    private var containerSize = IntSize.Zero
        set(value) {
            if (field.width != value.width || field.height != value.height) {
                field = value
                container.setBounds(0, 0, value.width, value.height)
                _mediator?.onChangeComponentSize()
            }
        }

    private var _mediator: ComposeSceneMediator? = null
    private var outsidePointerCallback: ((Boolean) -> Unit)? = null

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
            _mediator?.contentBounds = value.toAwtRectangle(container.density)
        }

    override var scrimColor: Color? = null
        set(value) {
            field = value
            val background = value ?: Color.Transparent
            container.background = background.toAwtColor()
        }

    init {
        bounds = IntRect(0, 0, window.width, window.height)
        _mediator = ComposeSceneMediator(
            container = container,
            windowContext = composeContainer.windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            coroutineContext = compositionContext.effectCoroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.transparency = true
            it.contentBounds = bounds.toAwtRectangle(density)
        }
        composeContainer.attachLayer(this)
    }

    override fun close() {
        composeContainer.detachLayer(this)
        _mediator?.dispose()
        _mediator = null
        window.remove(container)
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

    override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)?
    ) {
        outsidePointerCallback = onOutsidePointerEvent
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return SwingSkiaLayerAdapter(mediator, skiaLayerAnalytics)
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
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

    override fun onChangeWindowBounds() {
        containerSize = IntSize(window.width, window.height)
    }

    // region MouseListener

    override fun mouseClicked(event: MouseEvent) = Unit
    override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
    override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
    override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
    override fun mouseExited(event: MouseEvent) = onMouseEvent(event)

    // endregion

    private fun onMouseEvent(event: MouseEvent) {
        // TODO: Filter/consume based on [focused] flag
        val dismissRequest = event.button == MouseEvent.BUTTON1 && event.id == MouseEvent.MOUSE_RELEASED
        outsidePointerCallback?.invoke(dismissRequest)
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
