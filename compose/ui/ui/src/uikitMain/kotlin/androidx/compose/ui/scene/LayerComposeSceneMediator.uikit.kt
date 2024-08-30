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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectContainsPoint
import platform.UIKit.UIView

/**
 * Layout of sceneView on the screen
 */
data class LayerComposeSceneMediatorLayout(
    val renderRect: IntRect,
    val interactionRect: IntRect
)

/**
 * A mediator for the [ComposeScene] of a layer.
 * It shares its rendering view with other [LayerComposeSceneMediator].
 */
internal class LayerComposeSceneMediator(
    parentView: UIView,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack?,
    windowContext: PlatformWindowContext,
    coroutineContext: CoroutineContext,
    override val metalView: MetalView,
    private val onGestureEvent: (GestureEvent) -> Unit,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene
): ComposeSceneMediator(
    parentView,
    configuration,
    focusStack,
    windowContext,
    coroutineContext,
    composeSceneFactory
) {
    var boundsInWindow = IntRect.Zero

    override fun onGestureEvent(gestureEvent: GestureEvent) = onGestureEvent.invoke(gestureEvent)

    override fun isPointInsideInteractionBounds(point: CValue<CGPoint>): Boolean =
        boundsInWindow.contains(
            point
                .asDpOffset()
                .toOffset(view.density)
                .round()
        )

    fun render(canvas: Canvas, nanoTime: Long) {
        scene.render(canvas, nanoTime)
    }

    fun retrieveInteropTransaction(): UIKitInteropTransaction =
        interopContainer.retrieveTransaction()
}