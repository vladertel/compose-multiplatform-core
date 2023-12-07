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
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.skia.SkiaLayerAdapter
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.scaledSize
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.SkiaLayerAnalytics

internal val LocalLayerContainer = staticCompositionLocalOf<Container> {
    error("CompositionLocal LayerContainer not provided")
}

internal class ComposeContainer(
    val container: JLayeredPane,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
) : ComponentListener, WindowFocusListener {
    private val windowInfo = WindowInfoImpl()
    private var window: Window? = null
    private val layers = mutableListOf<DesktopComposeSceneLayer>()

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val mainScene = ComposeSceneMediator(
        container = container,
        windowInfo = windowInfo,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

    val contentComponent by mainScene::contentComponent
    val accessible by mainScene::accessible // TODO Another layers?
    var rootForTestListener by mainScene::rootForTestListener
    var fullscreen by mainScene::fullscreen // TODO: Dialogs in fullscreen?
    var compositionLocalContext by mainScene::compositionLocalContext
    var exceptionHandler: WindowExceptionHandler? = null
    val windowHandle by mainScene::windowHandle
    val renderApi by mainScene::renderApi
    val preferredSize by mainScene::preferredSize

    fun dispose() {
        mainScene.dispose()
        // Dispose layers
    }

    override fun componentResized(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentMoved(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentShown(e: ComponentEvent?) = Unit
    override fun componentHidden(e: ComponentEvent?) = Unit

    override fun windowGainedFocus(event: WindowEvent) = onChangeWindowFocus()
    override fun windowLostFocus(event: WindowEvent) = onChangeWindowFocus()

    private fun onChangeWindowFocus() {
        windowInfo.isWindowFocused = window?.isFocused ?: false
        mainScene.onChangeWindowFocus()
        // TODO: update layers
    }

    private fun onChangeWindowBounds() {
        val component = window ?: container
        val scaledSize = component.scaledSize
        if (windowInfo.containerSize != scaledSize) {
            windowInfo.containerSize = scaledSize
        }
    }

    fun onChangeWindowTransparency(value: Boolean) {
        // TODO: Consider to move to windowInfo (again)
        mainScene.isWindowTransparent = value
        // TODO: update layers
//        mainScene.transparency = value || interopBlending
    }

    fun onChangeLayoutDirection() {
        val layoutDirection = layoutDirectionFor(container)
        mainScene.onChangeLayoutDirection(layoutDirection)
        // TODO: update layers
    }

    fun addNotify() {
        mainScene.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))
    }

    fun removeNotify() {
        setWindow(null)
    }

    fun addToComponentLayer(component: Component) {
        mainScene.addToComponentLayer(component)
    }

    fun setSize(width: Int, height: Int) {
        mainScene.setSize(width, height)
    }

    private fun setWindow(window: Window?) {
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
        mainScene.setKeyEventListeners(onPreviewKeyEvent, onKeyEvent)
    }

    fun setContent(content: @Composable () -> Unit) {
        mainScene.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return if (ComposeFeatureFlags.useSwingGraphics) {
//            SwingSkiaLayerAdapter()
            TODO()
        } else {
            SkiaLayerAdapter(mediator, skiaLayerAnalytics)
        }
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        val layoutDirection = layoutDirectionFor(container)
        return if (ComposeFeatureFlags.usePlatformLayers) {
            SingleLayerComposeScene(
                coroutineContext = mediator.coroutineContext,
                density = density,
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
                composeSceneContext = ComposeSceneContextImpl(
                    platformContext = mediator.platformContext
                ),
            )
        } else {
            MultiLayerComposeScene(
                coroutineContext = mediator.coroutineContext,
                composeSceneContext = ComposeSceneContextImpl(
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
            WindowComposeSceneLayer()
        } else {
            SwingComposeSceneLayer()
        }
    }

    fun attachLayer(layer: DesktopComposeSceneLayer) {
        layers.add(layer)
    }

    fun detachLayer(layer: DesktopComposeSceneLayer) {
        layers.remove(layer)
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer = createPlatformLayer(
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
