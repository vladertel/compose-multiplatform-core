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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPGestureRecognizerDelegateProxy
import kotlin.experimental.ExperimentalObjCName
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIEvent
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerState
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGestureRecognizerStatePossible
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateRecognized
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.setState
import platform.darwin.NSObject

private fun UIGestureRecognizerState.asUIGestureRecognizerStateString(): String = when (this) {
    UIGestureRecognizerStatePossible -> "Possible"
    UIGestureRecognizerStateBegan -> "Began"
    UIGestureRecognizerStateChanged -> "Changed"
    UIGestureRecognizerStateEnded -> "Ended"
    UIGestureRecognizerStateCancelled -> "Cancelled"
    UIGestureRecognizerStateFailed -> "Failed"
    else -> "Unknown"
}

@OptIn(BetaInteropApi::class)
internal val NSObject.className: String
    get() {
        val thisClass = this.`class`() ?: return "null"
        return NSStringFromClass(thisClass)
            .split('.')
            .lastOrNull() ?: "UnknownClassName"
            // remove numbers in the end and prefix dashes
            .replace(Regex("\\d+$"), "")
            // remove prefix underscores
            .replace(Regex("^_+"), "")
    }

/**
 * The result of [DragAndDropSessionGatingGestureRecognizer.interrupt]. If the
 * gesture recognizers that are required to fail are in [UIGestureRecognizerStatePossible],
 * then we unlock their recognition and allow the drag and drop session to start (but can't give
 * a strong guarantee that it will start).
 *
 * If the gesture recognizers are in [UIGestureRecognizerStateFailed], then we can guarantee that
 * the drag and drop session will not start until the next gesture sequence and return
 */
enum class DragAndDropSessionGatingInterruptionOutcome {
    /**
     * The drag and drop session can start and is not gated by this gesture recognizer.
     */
    POTENTIAL_SUCCESS,

    /**
     * The gating gesture recognizer is not in the possible state, so the drag and drop session
     * can't be started until the next gesture sequence.
     */
    IMPOSSIBLE
}

/**
 * This class is a dummy gesture recognizer that is used to gate drag and drop events.
 * It is used to configure failure requirements for [UIGestureRecognizer]s created by
 * [UIDragInteraction].
 *
 * Until this fails explicitly in [interrupt], the drag and drop session can not be started by the system, because
 * all the touches will be redirected to this gesture recognizer first.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("DragAndDropSessionGatingGestureRecognizer")
internal class DragAndDropSessionGatingGestureRecognizer: CMPGestureRecognizer(target = null, action = null) {
    private val delegateProxy: CMPGestureRecognizerDelegateProxy = object : CMPGestureRecognizerDelegateProxy() {
        override fun gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer(
            gestureRecognizer: UIGestureRecognizer,
            otherGestureRecognizer: UIGestureRecognizer
        ): Boolean {
            require(gestureRecognizer == this@DragAndDropSessionGatingGestureRecognizer) {
                "Unexpected gesture recognizer ${gestureRecognizer.className} delegated to ${this@DragAndDropSessionGatingGestureRecognizer.className}"
            }

            return otherGestureRecognizer != gatedGestureRecognizer
        }

        override fun gestureRecognizerShouldBeRequiredToFailByGestureRecognizer(
            gestureRecognizer: UIGestureRecognizer,
            otherGestureRecognizer: UIGestureRecognizer
        ): Boolean {
            require(gestureRecognizer == this@DragAndDropSessionGatingGestureRecognizer) {
                "Unexpected ${gestureRecognizer.className} delegated to ${this@DragAndDropSessionGatingGestureRecognizer.className}"
            }

            if (gatedGestureRecognizer == null) {
                // We don't have the gated gesture recognizers set up yet
                return false
            }

            return if (gatedGestureRecognizer == otherGestureRecognizer) {
                // We require the gated gestures to wait for this gesture recognizer to fail
                println("${otherGestureRecognizer.className} requires failure of ${gestureRecognizer.className}: true")
                true
            } else {
                // We don't require other gesture recognizers to wait for this gesture recognizer to fail
                println("${otherGestureRecognizer.className} requires failure of ${gestureRecognizer.className}: false")
                false
            }
        }

        override fun gestureRecognizerShouldRequireFailureOfGestureRecognizer(
            gestureRecognizer: UIGestureRecognizer,
            otherGestureRecognizer: UIGestureRecognizer
        ): Boolean {
            require(gestureRecognizer == this@DragAndDropSessionGatingGestureRecognizer) {
                "Unexpected  ${gestureRecognizer.className} delegated to ${this@DragAndDropSessionGatingGestureRecognizer.className}"
            }

            // We don't require other gesture recognizers to fail
            println("${gestureRecognizer.className} requires failure of ${otherGestureRecognizer.className}: false")
            return false
        }
    }

    private var gatedGestureRecognizer: UILongPressGestureRecognizer? = null

    init {
        delegate = delegateProxy
        cancelsTouchesInView = false
        delaysTouchesEnded = false
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        println("Touches began")
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        println("Touches moved")
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        println("Touches ended")
        if (numberOfTouches == 0UL) {
            setState(UIGestureRecognizerStateRecognized)
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        println("Touches cancelled")
        if (numberOfTouches == 0UL) {
            setState(UIGestureRecognizerStateRecognized)
        }
    }

    /**
     * Mark this gesture recognizer as failed to allow native [UILongPressGestureRecognizer]s responsible
     * for [UIDragInteraction] to start.
     *
     * @return [DragAndDropSessionGatingInterruptionOutcome] that describes the outcome of the interruption.
     */
    fun interrupt(): DragAndDropSessionGatingInterruptionOutcome {
        if (state != UIGestureRecognizerStatePossible) {
            // Failure requirements can not be met if this gesture recognizer is not in the possible
            // state. It's either already failed and sits in cancelled state, or running, so the
            // drag and drop session can't start anyway.
            println("Can't interrupt")
            return DragAndDropSessionGatingInterruptionOutcome.IMPOSSIBLE
        }

        setState(UIGestureRecognizerStateFailed)

        println("Interrupted")
        return DragAndDropSessionGatingInterruptionOutcome.POTENTIAL_SUCCESS
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun dbgGesture(gestureRecognizer: UILongPressGestureRecognizer) {
        println("${gestureRecognizer.className} is in state ${gestureRecognizer.state.asUIGestureRecognizerStateString()}")
    }

    /**
     * Sets up the failure requirements for the [UILongPressGestureRecognizer]s that assumed to be backing the
     * [UIDragInteraction] logic in the view.
     */
    fun configure(interactionGestureRecognizer: UILongPressGestureRecognizer) {
        check(gatedGestureRecognizer == null) {
            "DragAndDropSessionGatingGestureRecognizer.configure() called multiple times"
        }

        gatedGestureRecognizer = interactionGestureRecognizer
        interactionGestureRecognizer.minimumPressDuration = 0.0
        //interactionGestureRecognizer.requireGestureRecognizerToFail(this)
        interactionGestureRecognizer.addTarget(this, NSSelectorFromString(::dbgGesture.name + ":"))
    }
}