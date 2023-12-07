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

import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class SkiaLayerAdapter(
    private val mediator: ComposeSceneMediator,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : SkiaLayerComponent {
    private val skiaLayer = object : SkiaLayer(
        externalAccessibleFactory = { mediator.accessible },
        analytics = skiaLayerAnalytics
    ) {
        init {
            skikoView = mediator.skikoView
        }

        override fun paint(g: Graphics) {
            mediator.onChangeComponentDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = mediator.currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            mediator.onChangeComponentSize()
        }

        override fun getPreferredSize(): Dimension = if (isPreferredSizeSet) {
            super.getPreferredSize()
        } else {
            mediator.preferredSize
        }
    }

    override val contentComponent
        get() = skiaLayer

    override val focusComponent by skiaLayer::canvas

    override val renderApi by skiaLayer::renderApi

    override val interopBlendingSupported
        get() = when(renderApi) {
            GraphicsApi.DIRECT3D, GraphicsApi.METAL -> true
            else -> false
        }

    override val clipComponents by skiaLayer::clipComponents

    override var transparency
        get() = skiaLayer.transparency
        set(value) {
            skiaLayer.transparency = value
            if (value && !mediator.isWindowTransparent && renderApi == GraphicsApi.METAL) {
                /*
                 * SkiaLayer sets background inside transparency setter, that is required for
                 * cases like software rendering.
                 * In case of transparent Metal canvas on opaque window, background values with
                 * alpha == 0 will make the result color black after clearing the canvas.
                 *
                 * Reset it to null to keep the color default.
                 */
                skiaLayer.background = null
            }
        }
    override var fullscreen by skiaLayer::fullscreen

    override val windowHandle by skiaLayer::windowHandle

    override fun dispose() {
        skiaLayer.dispose()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        skiaLayer.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeSceneInvalidate() {
        skiaLayer.needRedraw()
    }
}
