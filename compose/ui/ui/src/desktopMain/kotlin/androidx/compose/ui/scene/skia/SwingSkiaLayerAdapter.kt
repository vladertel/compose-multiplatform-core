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
import javax.accessibility.AccessibleContext
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.swing.SkiaSwingLayer

internal class SwingSkiaLayerAdapter(
    private val mediator: ComposeSceneMediator,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : SkiaLayerComponent {
    @OptIn(ExperimentalSkikoApi::class)
    private val skiaLayer = object : SkiaSwingLayer(
        skikoView = mediator.skikoView,
        analytics = skiaLayerAnalytics
    ) {
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

        override fun getAccessibleContext(): AccessibleContext? {
            return mediator.accessible.accessibleContext
        }
    }

    override val contentComponent
        get() = skiaLayer

    override val renderApi by skiaLayer::renderApi

    override val interopBlendingSupported: Boolean
        get() = true

    override val clipComponents by skiaLayer::clipComponents

    override var transparency
        get() = true
        set(_) {}

    override var fullscreen
        get() = false
        set(_) {}

    override val windowHandle get() = 0L

    override fun dispose() {
        skiaLayer.dispose()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        skiaLayer.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeSceneInvalidate() {
        skiaLayer.repaint()
    }

    override fun onRenderApiChanged(action: () -> Unit) {
    }
}
