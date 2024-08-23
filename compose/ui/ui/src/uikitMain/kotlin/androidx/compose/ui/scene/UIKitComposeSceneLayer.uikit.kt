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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.skiko.RecordDrawRectRenderDecorator
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.layoutConstraintsToMatch
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import androidx.compose.ui.window.centroidLocationInView
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UIView

/**
 * A backing ComposeSceneLayer view for each Compose scene layer. Its task is to
 * handle events that start outside the bounds of the layer content
 */
internal class ComposeSceneLayerView(
    val isInteractionViewHitTestSuccessful: (point: CValue<CGPoint>, withEvent: UIEvent?) -> Boolean,
    val isInsideInteractionBounds: (point: CValue<CGPoint>) -> Boolean,
    val isFocusable: () -> Boolean
): UIView(frame = CGRectZero.readValue()) {
    private var touchesCount: Int = 0
    private var previousSuccessHitTestTimestamp: Double? = null

    internal var onOutsidePointerEvent: ((
        eventType: PointerEventType
    ) -> Unit)? = null

    init {
        translatesAutoresizingMaskIntoConstraints = false
    }

    private fun touchStartedOutside(withEvent: UIEvent?) {
        println("touchStartedOutside event = $withEvent")
        // hitTest call can happen multiple times for the same touch event, ensure we only send
        // PointerEventType.Press once using the timestamp.
        if (previousSuccessHitTestTimestamp != withEvent?.timestamp) {
            // This workaround needs to send PointerEventType.Press just once
            previousSuccessHitTestTimestamp = withEvent?.timestamp
            onOutsidePointerEvent?.invoke(PointerEventType.Press)
        }
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        touchesCount += touches.size
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        touchesCount -= touches.size
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        touchesCount -= touches.size

        // It was the last touch in the sequence, calculate the centroid and if it's outside
        // the bounds, send `onOutsidePointerEvent`. Otherwise just return.
        if (touchesCount > 0) {
            return
        }

        val location = requireNotNull(
            touches
                .map { it as UITouch }
                .centroidLocationInView(this)
        ) {
            "touchesEnded should not be called with an empty set of touches"
        }

        if (!isInsideInteractionBounds(location)) {
            onOutsidePointerEvent?.invoke(PointerEventType.Release)
        }

        super.touchesEnded(touches, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        // TODO: why do we have two functions here?
        val isInBounds = isInteractionViewHitTestSuccessful(point, withEvent) && isInsideInteractionBounds(point)

        if (!isInBounds && super.hitTest(point, withEvent) == this) {
            touchStartedOutside(withEvent)

            if (isFocusable()) {
                // Focusable layers don't let touches pass through
                return this
            }
        }

        return null
    }
}

// TODO: perhaps make LayerComposeSceneMediator a ComposeSceneLayer?
internal class UIKitComposeSceneLayer(
    private val onClosed: (UIKitComposeSceneLayer) -> Unit,
    private val createComposeSceneContext: (PlatformContext) -> ComposeSceneContext,
    private val providingCompositionLocals: @Composable (@Composable () -> Unit) -> Unit,
    metalView: MetalView,
    onGestureEvent: (GestureEvent) -> Unit,
    private val initDensity: Density,
    private val initLayoutDirection: LayoutDirection,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack?,
    windowContext: PlatformWindowContext,
    compositionContext: CompositionContext,
) : ComposeSceneLayer {

    override var focusable: Boolean = focusStack != null

    val view = ComposeSceneLayerView(
        ::isInteractionViewHitTestSuccessful,
        ::isInsideInteractionBounds,
        isFocusable = { focusable }
    )

    val mediator =
        LayerComposeSceneMediator(
            view,
            configuration,
            focusStack,
            windowContext,
            coroutineContext = compositionContext.effectCoroutineContext,
            metalView,
            onGestureEvent = onGestureEvent,
            composeSceneFactory = ::createComposeScene
        )

    init {
        view.addSubview(mediator.view)
        NSLayoutConstraint.activateConstraints(
            mediator.view.layoutConstraintsToMatch(view)
        )
    }

    private fun isInteractionViewHitTestSuccessful(point: CValue<CGPoint>, event: UIEvent?): Boolean =
        mediator.hitTestInteractionView(point, event) != null

    private fun isInsideInteractionBounds(point: CValue<CGPoint>): Boolean =
        boundsInWindow.contains(point.asDpOffset().toOffset(view.density).round())

    /**
     * Bounds of real drawings based on previous renders.
     */
    private var drawBounds = IntRect.Zero

    /**
     * The maximum amount to inflate the [drawBounds] comparing to [boundsInWindow].
     */
    private var maxDrawInflate = IntRect.Zero

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene =
        PlatformLayersComposeScene(
            density = initDensity, // We should use the local density already set for the current layer.
            layoutDirection = initLayoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = createComposeSceneContext(platformContext),
            invalidate = invalidate,
        )

    val hasInvalidations by mediator::hasInvalidations

    override var density by mediator::density
    override var layoutDirection by mediator::layoutDirection
    override var boundsInWindow: IntRect = IntRect.Zero
        set(value) {
            field = value
            updateBounds()
        }
    override var compositionLocalContext: CompositionLocalContext? by mediator::compositionLocalContext

    override var scrimColor: Color? = null
        set(value) {
            if (field != value) {
                field = value
                value?.let {
                    scrimPaint.color = value
                }
            }
        }

    private val scrimPaint = Paint()

    fun render(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        if (scrimColor != null) {
            canvas.drawRect(
                left = 0f,
                top = 0f,
                right = width.toFloat(),
                bottom = height.toFloat(),
                paint = scrimPaint
            )
        }

        mediator.render(canvas, nanoTime)
    }

    override fun close() {
        onClosed(this)

        dispose()
    }

    internal fun dispose() {
        mediator.dispose()
        view.removeFromSuperview()
    }

    override fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            providingCompositionLocals {
                content()
            }
        }
    }

    override fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        mediator.setKeyEventListener(onPreviewKeyEvent, onKeyEvent)
    }

    override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((eventType: PointerEventType, button: PointerButton?) -> Unit)?
    ) {
        view.onOutsidePointerEvent = {
            onOutsidePointerEvent?.invoke(it, null)
        }
    }

    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset {
        return positionInWindow
    }

    private fun recordDrawBounds(renderDelegate: SkikoRenderDelegate) =
        RecordDrawRectRenderDecorator(renderDelegate) { canvasBoundsInPx ->
            val currentCanvasOffset = drawBounds.topLeft
            val drawBoundsInWindow = canvasBoundsInPx.roundToIntRect().translate(currentCanvasOffset)
            maxDrawInflate = maxInflate(boundsInWindow, drawBoundsInWindow, maxDrawInflate)
            drawBounds = IntRect(
                left = boundsInWindow.left - maxDrawInflate.left,
                top = boundsInWindow.top - maxDrawInflate.top,
                right = boundsInWindow.right + maxDrawInflate.right,
                bottom = boundsInWindow.bottom + maxDrawInflate.bottom
            )
            updateBounds()
        }

    private fun updateBounds() {
        mediator.setLayout(
            LayerComposeSceneMediatorLayout(
                renderRect = drawBounds,
                interactionRect = boundsInWindow
            )
        )
    }

    fun sceneDidAppear() {
        mediator.sceneDidAppear()
    }

    fun sceneWillDisappear() {
        mediator.sceneWillDisappear()
    }

    fun viewSafeAreaInsetsDidChange() {
        mediator.viewSafeAreaInsetsDidChange()
    }

    fun viewWillLayoutSubviews() {
        mediator.viewWillLayoutSubviews()
    }
}

private fun maxInflate(baseBounds: IntRect, currentBounds: IntRect, maxInflate: IntRect) = IntRect(
    left = max(baseBounds.left - currentBounds.left, maxInflate.left),
    top = max(baseBounds.top - currentBounds.top, maxInflate.top),
    right = max(currentBounds.right - baseBounds.right, maxInflate.right),
    bottom = max(currentBounds.bottom - baseBounds.bottom, maxInflate.bottom)
)
