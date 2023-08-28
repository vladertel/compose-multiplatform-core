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
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoPointer
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSCoder
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView

internal interface ComposeUIViewDelegate {
    fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean

    fun onPointerEvent(event: SkikoPointerEvent)

    fun onChangedActiveTouchesCount(touchesCount: Int)
}

private class ComposeGestureRecognizer: UIGestureRecognizer() {
}

@ExportObjCClass
internal class ComposeUIView : UIView {
    var delegate: ComposeUIViewDelegate? = null
    private var touchesCount = 0
        set(value) {
            if (field == value) {
                return
            }

            field = value

            delegate?.onChangedActiveTouchesCount(touchesCount)
        }

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    constructor() : super(frame = CGRectZero.readValue())

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        touchesCount += touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.DOWN))
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)

        touchesCount -= touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.UP))
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.MOVE))
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        touchesCount -= touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.UP))
        }
    }

    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean =
        delegate?.pointInside(point, withEvent) ?: super.pointInside(point, withEvent)

    private fun UIEvent.toSkikoPointerEvent(kind: SkikoPointerEventKind): SkikoPointerEvent {
        val pointers = touchesForView(this@ComposeUIView).orEmpty().map {
            val touch = it as UITouch
            val (x, y) = touch.locationInView(this@ComposeUIView).useContents { x to y }
            SkikoPointer(
                x = x,
                y = y,
                pressed = touch.isPressed,
                device = SkikoPointerDevice.TOUCH,
                id = touch.hashCode().toLong(),
                pressure = touch.force
            )
        }

        return SkikoPointerEvent(
            x = pointers.centroidX,
            y = pointers.centroidY,
            kind = kind,
            timestamp = (timestamp * 1_000).toLong(),
            pointers = pointers,
            platform = this
        )
    }
}

private val UITouch.isPressed
    get() =
        phase != UITouchPhase.UITouchPhaseEnded &&
            phase != UITouchPhase.UITouchPhaseCancelled

private val Iterable<SkikoPointer>.centroidX get() = asSequence().map { it.x }.average()
private val Iterable<SkikoPointer>.centroidY get() = asSequence().map { it.y }.average()