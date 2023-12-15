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

package androidx.compose.ui.scene.skia

import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer

/**
 * Provides a heavyweight AWT [component] used to render content (provided by client.skikoView) on-screen with Skia.
 *
 * If smooth interop with Swing is needed, consider using [SwingSkiaLayerComponent]
 */
internal class WindowSkiaLayerComponent(
    private val client: SkiaLayerComponent.Client
) : SkiaLayerComponent {
    /**
     * See also backend layer for swing interop in [SwingSkiaLayerComponent]
     */
    override val component: SkiaLayer = object : SkiaLayer(
        externalAccessibleFactory = { client.sceneAccessible },
        analytics = client.skiaLayerAnalytics
    ), Accessible {
        override fun addNotify() {
            super.addNotify()
            client.resetSceneDensity()
            client.initContent()
            client.updateSceneSize()
            client.setParentWindow(SwingUtilities.getWindowAncestor(this))
        }

        override fun removeNotify() {
            client.setParentWindow(null)
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            client.resetSceneDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = client.inputMethodRequests

        override fun doLayout() {
            super.doLayout()
            client.updateSceneSize()
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else client.scenePreferredSize
        }
    }

    override val renderApi: GraphicsApi
        get() = component.renderApi

    override val interopBlendingSupported: Boolean
        get() = when(renderApi) {
            GraphicsApi.DIRECT3D, GraphicsApi.METAL -> true
            else -> false
        }

    override val clipComponents: MutableList<ClipRectangle>
        get() = component.clipComponents

    override val focusComponentDelegate: Component
        get() = component.canvas

    override var transparency: Boolean
        get() = component.transparency
        set(value) {
            component.transparency = value
            if (value && !client.isWindowTransparent && renderApi == GraphicsApi.METAL) {
                /*
                 * SkiaLayer sets background inside transparency setter, that is required for
                 * cases like software rendering.
                 * In case of transparent Metal canvas on opaque window, background values with
                 * alpha == 0 will make the result color black after clearing the canvas.
                 *
                 * Reset it to null to keep the color default.
                 */
                component.background = null
            }
        }

    override var fullscreen: Boolean
        get() = component.fullscreen
        set(value) {
            component.fullscreen = value
        }

    override val windowHandle: Long get() = component.windowHandle

    init {
        component.skikoView = client.skikoView
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) =
        component.requestNativeFocusOnAccessible(accessible)

    override fun onComposeInvalidation() {
        component.needRedraw()
    }

    override fun onRenderApiChanged(action: () -> Unit) {
        component.onStateChanged(SkiaLayer.PropertyKind.Renderer) { action() }
    }

    override fun dispose() {
        component.dispose()
    }
}
