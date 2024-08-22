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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.ExclusiveLayoutConstraints
import androidx.compose.ui.uikit.layoutConstraintsToCenterInParent
import androidx.compose.ui.uikit.layoutConstraintsToMatch
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectContainsPoint
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

private class MetalViewRenderer(
    private val scene: ComposeScene,
    private val sceneOffset: () -> Offset,
) : SkikoRenderDelegate {
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        canvas.withSceneOffset {
            scene.render(asComposeCanvas(), nanoTime)
        }
    }

    private inline fun Canvas.withSceneOffset(block: Canvas.() -> Unit) {
        val sceneOffset = sceneOffset()
        save()
        translate(sceneOffset.x, sceneOffset.y)
        block()
        restore()
    }
}

internal sealed interface PrimaryComposeSceneMediatorLayout {
    data object Fill : PrimaryComposeSceneMediatorLayout
    class Center(val size: CValue<CGSize>) : PrimaryComposeSceneMediatorLayout
}

/**
 * A mediator for the primary [ComposeScene], that owns its own rendering view.
 */
internal class PrimaryComposeSceneMediator(
    parentView: UIView,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack?,
    windowContext: PlatformWindowContext,
    measureDrawLayerBounds: Boolean = false,
    coroutineContext: CoroutineContext,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene
) : ComposeSceneMediator(
    parentView,
    configuration,
    focusStack,
    windowContext,
    measureDrawLayerBounds,
    coroutineContext,
    composeSceneFactory
) {
    override val metalView: MetalView = MetalView(
        renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                scene.render(canvas.asComposeCanvas(), nanoTime)
            }
        },
        retrieveInteropTransaction = {
            interopContainer.retrieveTransaction()
        }
    )

    private val constraints = ExclusiveLayoutConstraints()

    init {
        interactionView.addSubview(metalView)
    }

    /**
     * When there is an ongoing gesture, we need notify redrawer about it. It should unconditionally
     * unpause CADisplayLink which affects frequency of polling UITouch events on high frequency
     * display and force it to match display refresh rate.
     *
     * Otherwise [UIEvent]s will be dispatched with the 60hz frequency.
     */
    override fun onGestureEvent(gestureEvent: GestureEvent) {
        val needHighFrequencyPolling =
            when (gestureEvent) {
                GestureEvent.BEGAN -> true
                GestureEvent.ENDED -> false
            }
        metalView.needsProactiveDisplayLink = needHighFrequencyPolling
    }

    override fun isPointInsideInteractionBounds(point: CValue<CGPoint>): Boolean =
        CGRectContainsPoint(interactionView.bounds, point)

    fun setLayout(value: PrimaryComposeSceneMediatorLayout) {
        when (value) {
            PrimaryComposeSceneMediatorLayout.Fill -> {
                metalView.translatesAutoresizingMaskIntoConstraints = false
                constraints.set(
                    metalView.layoutConstraintsToMatch(interactionView)
                )
            }

            is PrimaryComposeSceneMediatorLayout.Center -> {
                metalView.translatesAutoresizingMaskIntoConstraints = false
                constraints.set(
                    metalView.layoutConstraintsToCenterInParent(parentView, value.size)
                )
            }
        }
    }

    fun performOrientationChangeAnimation(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        val startSnapshotView = view.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        parentView.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor)
                )
            )
        }

        metalView.isForcedToPresentWithTransactionEveryFrame = true

        setLayout(PrimaryComposeSceneMediatorLayout.Center(targetSize))
        metalView.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                metalView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(PrimaryComposeSceneMediatorLayout.Fill)
                metalView.isForcedToPresentWithTransactionEveryFrame = false
            }
        )

        view.layoutIfNeeded()
    }
}