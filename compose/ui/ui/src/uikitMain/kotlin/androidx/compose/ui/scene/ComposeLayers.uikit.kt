/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropMutableTransaction
import androidx.compose.ui.window.MetalView
import kotlinx.cinterop.CValue
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGSize
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

internal class ComposeLayers: SkikoRenderDelegate {
    val hasInvalidations: Boolean
        get() = layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()

    val metalView: MetalView = MetalView(
        renderDelegate = this,
        retrieveInteropTransaction = {
            // TODO: proper implementation
            UIKitInteropMutableTransaction()
        }
    )

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        TODO()
    }

    fun dispose(hasViewAppeared: Boolean) {
        metalView.dispose()

        // `dispose` is called instead of `close`, because `close` is also used imperatively
        // to remove the layer from the array.
        while (layers.isNotEmpty()) {
            val layer = layers.removeLast()

            if (hasViewAppeared) {
                layer.sceneWillDisappear()
            }

            layer.dispose()
        }
    }

    fun attach(layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        layers.add(layer)

        if (hasViewAppeared) {
            layer.sceneDidAppear()
        }
    }

    fun detach(layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        if (hasViewAppeared) {
            layer.sceneWillDisappear()
        }
        layers.remove(layer)
    }

    // TODO: investigate correctness of these implementations
    fun viewSafeAreaInsetsDidChange() {
        layers.fastForEach {
            it.viewSafeAreaInsetsDidChange()
        }
    }

    fun viewWillLayoutSubviews() {
        layers.fastForEach {
            it.viewWillLayoutSubviews()
        }
    }

    fun viewDidAppear() {
        layers.fastForEach {
            it.sceneDidAppear()
        }
    }

    fun viewWillDisappear() {
        layers.fastForEach {
            it.sceneWillDisappear()
        }
    }
}