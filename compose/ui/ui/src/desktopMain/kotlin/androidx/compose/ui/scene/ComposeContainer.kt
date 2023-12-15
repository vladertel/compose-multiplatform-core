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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeBridge
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.swing.JLayeredPane
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Internal entry point to Compose.
 *
 * It binds Skia canvas and [ComposeScene] to [container].
 * It also configures compose based on [ComposeFeatureFlags].
 */
internal class ComposeContainer(
    private val container: JLayeredPane,
    skiaLayerAnalytics: SkiaLayerAnalytics,
    private var layoutDirection: LayoutDirection,
    createSkiaLayerComponent: (SkiaLayerComponent.Client) -> SkiaLayerComponent,
) {
    private val bridge = ComposeBridge(
        skiaLayerAnalytics,
        createSkiaLayerComponent,
        ::createComposeScene
    )

    /**
     * A list of additional layers. Layers are used to render [Popup]s and [Dialog]s.
     */
    private val layers = mutableListOf<DesktopComposeSceneLayer>()
    private val layersContainer get() = container

    val accessible get() = bridge.sceneAccessible
    val windowContext get() = bridge.windowContext
    var rootForTestListener by bridge::rootForTestListener
    var fullscreen by bridge.skiaLayerComponent::fullscreen
    var compositionLocalContext by bridge::compositionLocalContext
    @ExperimentalComposeUiApi
    var exceptionHandler by bridge::exceptionHandler
    val windowHandle get() = bridge.skiaLayerComponent.windowHandle
    val renderApi get() = bridge.skiaLayerComponent.renderApi
    val preferredSize: Dimension? get() = bridge.component.preferredSize
    val focusManager get() = bridge.focusManager
    val contentComponent get() = bridge.component

    private val interopBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending &&
            bridge.skiaLayerComponent.interopBlendingSupported

    init {
        bridge.skiaLayerComponent.transparency = interopBlending
        container.layout = null
        addToLayer(bridge.invisibleComponent, bridgeLayer)
        addToLayer(bridge.component, bridgeLayer)
    }

    fun dispose() {
        bridge.dispose()
        container.remove(bridge.component)
        container.remove(bridge.invisibleComponent)
        layers.fastForEach(DesktopComposeSceneLayer::close)
    }

    fun onChangeWindowTransparency(value: Boolean) {
        bridge.isWindowTransparent = value
        bridge.skiaLayerComponent.transparency = value || interopBlending
    }

    fun onChangeLayoutDirection(component: Component) {
        layoutDirection = layoutDirectionFor(component)
        bridge.layoutDirection = layoutDirection
    }

    fun onRenderApiChanged(action: () -> Unit) {
        bridge.skiaLayerComponent.onRenderApiChanged(action)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        bridge.component.setBounds(x, y, width, height)
    }

    fun addToComponentLayer(component: Component) {
        addToLayer(component, componentLayer)
        if (!interopBlending) {
            bridge.addClipComponent(component)
        }
    }

    fun removeFromComponentLayer(component: Component) {
        bridge.removeClipComponent(component)
    }

    private fun addToLayer(component: Component, layer: Int) {
        if (renderApi == GraphicsApi.METAL) {
            // Applying layer on macOS makes our bridge non-transparent
            // But it draws always on top, so we can just add it as-is
            // TODO: Figure out why it makes difference in transparency
            container.add(component, 0)
        } else {
            container.setLayer(component, layer)
            container.add(component, null, -1)
        }
    }

    private val bridgeLayer: Int get() = 10
    private val componentLayer: Int
        get() = if (interopBlending) 0 else 20

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        bridge.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
    }

    fun setContent(content: @Composable () -> Unit) {
        bridge.setContent {
            CompositionLocalProvider(
                LocalLayerContainer provides container,
                content = content
            )
        }
    }

    fun addMouseListener(listener: MouseListener) =
        bridge.component.addMouseListener(listener)

    fun removeMouseListener(listener: MouseListener) =
        bridge.component.removeMouseListener(listener)

    fun addMouseMotionListener(listener: MouseMotionListener) =
        bridge.component.addMouseMotionListener(listener)

    fun removeMouseMotionListener(listener: MouseMotionListener) =
        bridge.component.removeMouseMotionListener(listener)

    fun addMouseWheelListener(listener: MouseWheelListener) =
        bridge.component.addMouseWheelListener(listener)

    fun removeMouseWheelListener(listener: MouseWheelListener) =
        bridge.component.removeMouseWheelListener(listener)

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene = if (ComposeFeatureFlags.usePlatformLayers) {
        SingleLayerComposeScene(
            coroutineContext = coroutineContext,
            density = container.density,
            invalidate = invalidate,
            layoutDirection = layoutDirection,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
        )
    } else {
        MultiLayerComposeScene(
            coroutineContext = coroutineContext,
            density = container.density,
            invalidate = invalidate,
            layoutDirection = layoutDirection,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
        )
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer = if (ComposeFeatureFlags.useWindowLayers) {
            WindowComposeSceneLayer(
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                scrimColor = null,
                bounds = IntRect.Zero,
                compositionContext = compositionContext,
            )
        } else {
            SwingComposeSceneLayer(
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                bounds = IntRect.Zero,
                scrimColor = null,
                compositionContext = compositionContext,
            )
        }
    }

    internal abstract class DesktopComposeSceneLayer : ComposeSceneLayer {
        open fun onChangeWindowFocus() = Unit
        open fun onChangeWindowBounds() = Unit
    }

    private inner class SwingComposeSceneLayer(
        override var density: Density,
        override var layoutDirection: LayoutDirection,
        override var focusable: Boolean,
        override var bounds: IntRect,
        override var scrimColor: Color?,
        compositionContext: CompositionContext,
    ) : DesktopComposeSceneLayer() {
        init {
            layers.add(this)
        }

        override fun close() {
            layers.remove(this)
        }

        override fun setContent(content: @Composable () -> Unit) {
        }

        override fun setKeyEventListener(
            onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
            onKeyEvent: ((KeyEvent) -> Boolean)?
        ) {
        }

        override fun setOutsidePointerEventListener(
            onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)?
        ) {
        }
    }

    private inner class WindowComposeSceneLayer(
        override var density: Density,
        override var layoutDirection: LayoutDirection,
        override var focusable: Boolean,
        override var bounds: IntRect,
        override var scrimColor: Color?,
        compositionContext: CompositionContext,
    ) : DesktopComposeSceneLayer() {
        init {
            layers.add(this)
        }

        override fun close() {
            layers.remove(this)
        }

        override fun setContent(content: @Composable () -> Unit) {
        }

        override fun setKeyEventListener(
            onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
            onKeyEvent: ((KeyEvent) -> Boolean)?
        ) {
        }

        override fun setOutsidePointerEventListener(onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)?) {
        }

        override fun onChangeWindowBounds() {
        }
    }
}
