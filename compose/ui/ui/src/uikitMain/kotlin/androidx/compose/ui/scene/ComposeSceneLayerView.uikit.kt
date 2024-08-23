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

import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.window.centroidLocationInView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UIView

/**
 * A backing ComposeSceneLayer view for each Compose scene layer. Its task is to
 * handle events that start outside the bounds of the layer content
 */
internal class ComposeSceneLayerView(
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
        // the bounds, send `onOutsidePointerEvent`. Otherwise, just return.
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
        val isOutsideBounds = !isInsideInteractionBounds(point)

        if (isOutsideBounds && super.hitTest(point, withEvent) == this) {
            touchStartedOutside(withEvent)

            if (isFocusable()) {
                // Focusable layers don't let touches pass through
                return this
            }
        }

        return null
    }
}