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

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.uikit.layoutConstraintsToMatch
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropMutableTransaction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import kotlinx.cinterop.readValue
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import platform.UIKit.UIWindow

internal class ComposeLayersView: UIView(frame = CGRectZero.readValue())

internal class ComposeLayers: SkikoRenderDelegate {
    val hasInvalidations: Boolean
        get() = layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()
    private var ongoingGesturesCount = 0

    val view = ComposeLayersView()

    val metalView: MetalView = MetalView(
        renderDelegate = this,
        retrieveInteropTransaction = ::retrieveAndMergeInteropTransactions
    ).apply {
        canBeOpaque = false
    }

    init {
        view.translatesAutoresizingMaskIntoConstraints = false

        metalView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(metalView)

        NSLayoutConstraint.activateConstraints(
            metalView.layoutConstraintsToMatch(view)
        )
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val composeCanvas = canvas.asComposeCanvas()

        layers.fastForEach {
            it.render(composeCanvas, width, height, nanoTime)
        }
    }

    /**
     * When there is an ongoing gesture, we need notify redrawer about it. It should unconditionally
     * unpause CADisplayLink which affects frequency of polling UITouch events on high frequency
     * display and force it to match display refresh rate.
     *
     * Otherwise [UIEvent]s will be dispatched with the 60hz frequency.
     */
    fun onGestureEvent(event: GestureEvent) {
        val hadAnyOngoingGestures = ongoingGesturesCount > 0

        when (event) {
            GestureEvent.BEGAN -> {
                ongoingGesturesCount++

                if (!hadAnyOngoingGestures && ongoingGesturesCount > 0) {
                    metalView.needsProactiveDisplayLink = true
                }
            }
            GestureEvent.ENDED -> {
                ongoingGesturesCount--

                if (hadAnyOngoingGestures && ongoingGesturesCount == 0) {
                    metalView.needsProactiveDisplayLink = false
                }
            }
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

        view.insertSubview(layer.view, belowSubview = metalView)
        NSLayoutConstraint.activateConstraints(
            layer.view.layoutConstraintsToMatch(view)
        )

        if (isFirstLayer) {
            // The content of previous layers drawn on the Metal view should be cleared and
            // redrawn synchronously after the new layer is attached to avoid flickering.

            metalView.setNeedsSynchronousDraw()

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

    // End of suspicious code

    /**
     * Iterate through existing layers and merge their interop transactions to be consumed by the
     * [MetalView]
     */
    private fun retrieveAndMergeInteropTransactions(): UIKitInteropTransaction {
        // TODO: proper implementation
        return UIKitInteropMutableTransaction()
    }
}