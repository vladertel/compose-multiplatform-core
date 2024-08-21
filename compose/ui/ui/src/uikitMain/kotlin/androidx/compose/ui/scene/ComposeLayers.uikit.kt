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
import kotlinx.cinterop.readValue
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.UIKit.UIWindow

internal class ComposeLayersView: UIView(frame = CGRectZero.readValue())

internal class ComposeLayers: SkikoRenderDelegate {
    val hasInvalidations: Boolean
        get() = layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()

    val view = ComposeLayersView()

    val metalView: MetalView = MetalView(
        renderDelegate = this,
        retrieveInteropTransaction = {
            // TODO: proper implementation
            UIKitInteropMutableTransaction()
        }
    )

    init {
        view.translatesAutoresizingMaskIntoConstraints = false

        metalView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(metalView)

        NSLayoutConstraint.activateConstraints(
            metalView.layoutConstraintsToMatch(view)
        )
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        layers.fastForEach {
            it.render()
        }
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

        view.removeFromSuperview()
    }

    fun attach(window: UIWindow, layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        val isFirstLayer = layers.isEmpty()

        layers.add(layer)

        if (isFirstLayer) {
            window.addSubview(view)
            NSLayoutConstraint.activateConstraints(
                view.layoutConstraintsToMatch(window)
            )
            window.layoutIfNeeded()
        }

        if (hasViewAppeared) {
            layer.sceneDidAppear()
        }
    }

    fun detach(layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        if (hasViewAppeared) {
            layer.sceneWillDisappear()
        }
        layers.remove(layer)

        if (layers.isEmpty()) {
            view.removeFromSuperview()
        }
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