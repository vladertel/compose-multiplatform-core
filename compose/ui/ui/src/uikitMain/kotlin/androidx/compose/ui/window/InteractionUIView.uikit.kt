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

package androidx.compose.ui.window

import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.darwin.NSObject
import androidx.compose.ui.uikit.utils.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectEqualToRect
import platform.UIKit.UIGestureRecognizer

internal enum class TouchesPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

private data class HitTestTarget(
    val view: UIView,
    val initialRect: CValue<CGRect>,
) {
    fun rectIsEqualToInitial(otherView: UIView): Boolean {
        val currentRect = view.convertRect(view.bounds, toView = otherView)

        return CGRectEqualToRect(initialRect, currentRect)
    }
}

private class GestureRecognizerProxy(
    private val updateTouchesCount: (count: Int) -> Unit,
    private val touchesDelegate: InteractionUIView.Delegate,
    private val view: UIView
) : NSObject(), CMPGestureRecognizerProxyProtocol {
    /**
     * The actual hit test target, that we can conditionally forward touch events to.
     * Needed to dynamically handle touches in both Compose and UIKit.
     */
    var hitTestTarget: HitTestTarget? = null

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    var touchesCount = 0
        private set(value) {
            field = value
            updateTouchesCount(value)
        }

    override fun shouldRecognizeSimultaneously(
        gestureRecognizer: UIGestureRecognizer,
        withOtherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return true
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        touchesCount += touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(view, event, TouchesPhase.BEGAN)
        }

        // If we have a hit test target, we need to forward the touch events to it.
        hitTestTarget?.let { target ->
            target.view.touchesBegan(
                touches = touches,
                withEvent = withEvent
            )
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(view, event, TouchesPhase.MOVED)
        }

        hitTestTarget?.let { target ->
            if (target.rectIsEqualToInitial(view)) {
                target.view.touchesMoved(
                    touches = touches,
                    withEvent = withEvent
                )
            } else {
                target.view.touchesCancelled(
                    touches = touches,
                    withEvent = withEvent
                )

                hitTestTarget = null
            }
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        touchesCount -= touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(view, event, TouchesPhase.ENDED)
        }

        hitTestTarget?.let { target ->
            target.view.touchesEnded(
                touches = touches,
                withEvent = withEvent
            )
        }

        if (touchesCount == 0) {
            hitTestTarget = null
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        touchesCount -= touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(view, event, TouchesPhase.CANCELLED)
        }

        hitTestTarget?.let { target ->
            target.view.touchesCancelled(
                touches = touches,
                withEvent = withEvent
            )
        }

        if (touchesCount == 0) {
            hitTestTarget = null
        }
    }
}

internal class InteractionUIView(
    private var keyboardEventHandler: KeyboardEventHandler,
    private var touchesDelegate: Delegate,
    private var updateTouchesCount: (count: Int) -> Unit,
    private var inBounds: (CValue<CGPoint>) -> Boolean,
) : UIView(CGRectZero.readValue()) {
    interface Delegate {
        fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean
        fun onTouchesEvent(view: UIView, event: UIEvent, phase: TouchesPhase)
    }

    private var gestureRecognizerProxy: GestureRecognizerProxy? = null

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        gestureRecognizerProxy = GestureRecognizerProxy(updateTouchesCount, touchesDelegate, this)

        val gestureRecognizer = CMPGestureRecognizer()
        gestureRecognizer.proxy = gestureRecognizerProxy
        addGestureRecognizer(gestureRecognizer)
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        keyboardEventHandler.pressesBegan(presses, withEvent)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        keyboardEventHandler.pressesEnded(presses, withEvent)
        super.pressesEnded(presses, withEvent)
    }

    /**
     * https://developer.apple.com/documentation/uikit/uiview/1622533-point
     */
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean {
        return inBounds(point) && touchesDelegate.pointInside(point, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val view = super.hitTest(point, withEvent) ?: return null

        gestureRecognizerProxy?.let { proxy ->
            // Multitarget touches forwarding is not supported.
            if (proxy.touchesCount > 0) {
                return@let
            }

            if (view == this) {
                proxy.hitTestTarget = null
            } else {
                proxy.hitTestTarget = HitTestTarget(
                    view = view,
                    initialRect = view.convertRect(view.bounds, toView = this)
                )
            }
        }

        // hitTest is called after pointInside, so we can safely return this here
        return this
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        gestureRecognizerProxy = null
        touchesDelegate = object : Delegate {
            override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean = false
            override fun onTouchesEvent(view: UIView, event: UIEvent, phase: TouchesPhase) {}
        }
        updateTouchesCount = {}
        inBounds = { false }
        keyboardEventHandler = KeyboardEventHandler.Empty
    }
}
