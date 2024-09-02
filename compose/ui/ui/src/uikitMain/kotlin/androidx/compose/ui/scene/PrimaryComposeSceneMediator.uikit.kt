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
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.layoutConstraintsToCenterInParent
import androidx.compose.ui.uikit.layoutConstraintsToMatch
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

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
    coroutineContext,
    composeSceneFactory
) {
    override val metalView: MetalView = MetalView(
        retrieveInteropTransaction = interopContainer::retrieveTransaction,
        render = { canvas, nanoTime ->
            scene.render(canvas.asComposeCanvas(), nanoTime)
        }
    )

    init {
        // metalView layout is set in [setLayout] by the owner of this mediator
        metalView.translatesAutoresizingMaskIntoConstraints = false
        userInputView.addSubview(metalView)

        NSLayoutConstraint.activateConstraints(
            metalView.layoutConstraintsToMatch(userInputView)
        )
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

    /**
     * In the primary mediator [userInputView] [UIView.hitTest] happens after default [UIView.pointInside]
     * check, which is the only requirement.
     */
    override fun isPointInsideInteractionBounds(point: CValue<CGPoint>): Boolean = true

    fun setLayout(value: PrimaryComposeSceneMediatorLayout) {
        when (value) {
            PrimaryComposeSceneMediatorLayout.Fill -> {
                userInputViewConstraints.set(
                    userInputView.layoutConstraintsToMatch(view)
                )
            }

            is PrimaryComposeSceneMediatorLayout.Center -> {
                userInputViewConstraints.set(
                    userInputView.layoutConstraintsToCenterInParent(view, value.size)
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
        userInputView.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                userInputView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(PrimaryComposeSceneMediatorLayout.Fill)
                metalView.isForcedToPresentWithTransactionEveryFrame = false
            }
        )

        userInputView.setNeedsLayout()
        view.layoutIfNeeded()
    }
}