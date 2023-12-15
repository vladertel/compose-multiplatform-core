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

import androidx.compose.ui.ComposeFeatureFlags
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Provides a lightweight Swing [component] used to render content (provided by client.skikoView) on-screen with Skia.
 *
 * [SwingSkiaLayerComponent] provides smooth integration with Swing, so z-ordering, double-buffering etc. from Swing will be taken into account.
 *
 * However, if smooth interop with Swing is not needed, consider using [WindowSkiaLayerComponent]
 */
@OptIn(ExperimentalSkikoApi::class)
internal class SwingSkiaLayerComponent(
    private val client: SkiaLayerComponent.Client
) : SkiaLayerComponent {
    /**
     * See also backendLayer for standalone Compose in [WindowSkiaLayerComponent]
     */
    override val component: SkiaSwingLayer =
        object : SkiaSwingLayer(skikoView = client.skikoView, analytics = client.skiaLayerAnalytics) {
            override fun addNotify() {
                super.addNotify()
                client.resetSceneDensity()
                client.initContent()
                client.updateSceneSize()
                client.setParentWindow(SwingUtilities.getWindowAncestor(this))
            }

            override fun removeNotify() {
                client.setParentWindow(window = null)
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

            override fun getAccessibleContext(): AccessibleContext? {
                return client.sceneAccessible.accessibleContext
            }
        }

    override val renderApi: GraphicsApi
        get() = component.renderApi

    override val interopBlendingSupported: Boolean
        get() = ComposeFeatureFlags.useSwingGraphics

    override val clipComponents: MutableList<ClipRectangle>
        get() = component.clipComponents

    override val focusComponentDelegate: Component
        get() = component

    override var transparency: Boolean
        get() = true
        set(_) {}

    override var fullscreen: Boolean
        get() = false
        set(_) {}

    override val windowHandle get() = 0L

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        component.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeInvalidation() {
        component.repaint()
    }

    override fun onRenderApiChanged(action: () -> Unit) = Unit

    override fun dispose() {
        component.dispose()
    }
}