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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerAdapter
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerAdapter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.scaledSize
import java.awt.Component
import java.awt.Container
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.HierarchyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.SkiaLayerAnalytics

internal val LocalLayerContainer = staticCompositionLocalOf<Container> {
    error("CompositionLocal LayerContainer not provided")
}

internal class ComposeContainer(
    val container: JLayeredPane,
    val skiaLayerAnalytics: SkiaLayerAnalytics,
    window: Window? = null,
) : ComponentListener, WindowFocusListener {
    val windowContext = PlatformWindowContext()
    var window: Window? = window
        private set
    private var layoutDirection = layoutDirectionFor(window ?: container)
    private val layers = mutableListOf<DesktopComposeSceneLayer>()

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val mediator = ComposeSceneMediator(
        container = container,
        windowContext = windowContext,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

    val contentComponent by mediator::contentComponent
    val focusManager by mediator::focusManager
    val accessible by mediator::accessible
    var rootForTestListener by mediator::rootForTestListener
    // TODO: Changing fullscreen probably will require recreate our layers
    //  It will require add this flag as remember parameters in rememberComposeSceneLayer
    var fullscreen by mediator::fullscreen
    var compositionLocalContext by mediator::compositionLocalContext
    var exceptionHandler: WindowExceptionHandler? = null
    val windowHandle by mediator::windowHandle
    val renderApi by mediator::renderApi
    val preferredSize by mediator::preferredSize

    init {
        window?.addComponentListener(this)
    }

    fun dispose() {
        mediator.dispose()
        layers.fastForEach(DesktopComposeSceneLayer::close)
    }

    override fun componentResized(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentMoved(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentShown(e: ComponentEvent?) = Unit
    override fun componentHidden(e: ComponentEvent?) = Unit

    override fun windowGainedFocus(event: WindowEvent) = onChangeWindowFocus()
    override fun windowLostFocus(event: WindowEvent) = onChangeWindowFocus()

    private fun onChangeWindowFocus() {
        windowContext.setWindowFocused(window?.isFocused ?: false)
        mediator.onChangeWindowFocus()
        layers.fastForEach(DesktopComposeSceneLayer::onChangeWindowFocus)
    }

    private fun onChangeWindowBounds() {
        val component = window ?: container
        windowContext.setContainerSize(component.scaledSize)
        layers.fastForEach(DesktopComposeSceneLayer::onChangeWindowBounds)
    }

    fun onChangeWindowTransparency(value: Boolean) {
        windowContext.isWindowTransparent = value
        mediator.transparency = value
    }

    fun onChangeLayoutDirection(component: Component) {
        // ComposeWindow and ComposeDialog relies on self orientation, not on container's one
        layoutDirection = layoutDirectionFor(component)
        mediator.onChangeLayoutDirection(layoutDirection)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        mediator.onRenderApiChanged(action)
    }

    fun addNotify() {
        mediator.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))
    }

    fun removeNotify() {
        setWindow(null)
    }

    fun addToComponentLayer(component: Component) {
        mediator.addToComponentLayer(component)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        mediator.contentBounds = Rectangle(x, y, width, height)
    }

    private fun setWindow(window: Window?) {
        if (this.window == window) {
            return
        }

        this.window?.removeWindowFocusListener(this)
        this.window?.removeComponentListener(this)

        window?.addComponentListener(this)
        window?.addWindowFocusListener(this)
        this.window = window

        onChangeWindowFocus()
        onChangeWindowBounds()
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        mediator.setKeyEventListeners(onPreviewKeyEvent, onKeyEvent)
    }

    fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return if (ComposeFeatureFlags.useSwingGraphics) {
            SwingSkiaLayerAdapter(mediator, skiaLayerAnalytics)
        } else {
            SkiaLayerAdapter(mediator, windowContext, skiaLayerAnalytics)
        }
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return if (ComposeFeatureFlags.usePlatformLayers) {
            SingleLayerComposeScene(
                coroutineContext = mediator.coroutineContext,
                density = density,
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
                composeSceneContext = createComposeSceneContext(
                    platformContext = mediator.platformContext
                ),
            )
        } else {
            MultiLayerComposeScene(
                coroutineContext = mediator.coroutineContext,
                composeSceneContext = createComposeSceneContext(
                    platformContext = mediator.platformContext
                ),
                density = density,
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
        }
    }

    private fun createPlatformLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext
    ): ComposeSceneLayer {
        return if (ComposeFeatureFlags.useWindowLayers) {
            WindowComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext
            )
        } else {
            SwingComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext
            )
        }
    }

    fun attachLayer(layer: DesktopComposeSceneLayer) {
        layers.add(layer)
    }

    fun detachLayer(layer: DesktopComposeSceneLayer) {
        layers.remove(layer)
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer = this@ComposeContainer.createPlatformLayer(
            density = density,
            layoutDirection = layoutDirection,
            focusable = focusable,
            compositionContext = compositionContext
        )
    }

    private inner class DesktopCoroutineExceptionHandler :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }
}

@Composable
private fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(
    LocalLayerContainer provides composeContainer.container,
    content = content
)

internal val HierarchyEvent.isParentChanged
    get() = (changeFlags and HierarchyEvent.PARENT_CHANGED.toLong()) != 0L
