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
private val NSObject.className: String
    get() {
        val thisClass = this.`class`() ?: return "null"
        return NSStringFromClass(thisClass)
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
     * The drag and drop session is not possible, because one of the system gesture recognizers
     * that are required to fail can't begin during this gesture sequence anyway.
     */
    REDUNDANT_GATING,

    /**
     * The gating gesture recognizer is either not possible, or already running, so the drag and drop
     * session can't start either way, because failure requirements are not met.
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
    private var gatedGestureRecognizers: HashSet<UIGestureRecognizer>? = null

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    /**
     * If the state of this gesture recognizer is [UIGestureRecognizerStateBegan] and there are no touches,
     * we can set it to [UIGestureRecognizerStateEnded].
     *
     * This happens only if we were required to fail the gated gesture recognizers explicitly.
     *
     * @see [failGatedGestureRecognisers]
     */
    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        if (state == UIGestureRecognizerStateBegan && numberOfTouches == 0UL) {
            setState(UIGestureRecognizerStateEnded)
        }
    }

    /**
     * If the state of this gesture recognizer is [UIGestureRecognizerStateBegan] and there are no touches,
     * we can set it to [UIGestureRecognizerStateCancelled].
     *
     * This happens only if we were required to fail the gated gesture recognizers explicitly.
     *
     * @see [failGatedGestureRecognisers]
     */
    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        if (state == UIGestureRecognizerStateBegan && numberOfTouches == 0UL) {
            setState(UIGestureRecognizerStateCancelled)
        }
    }

    /**
     * Mark this gesture recognizer as failed to allow native [UILongPressGestureRecognizer]s responsible
     * for [UIDragInteraction] to start.
     *
     * @param performSessionSetup block that will be executed if all the gated gesture recognizers
     * conform to precondition to start the drag and drop session in the first place.
     * This block is required to be executed before this gesture recognizer is failed, because
     * gated gesture recognizers can start immediately after this gesture recognizer fails.
     * The required context setup for possible consequent drag and drop session should be done in this block.
     *
     * @return [DragAndDropSessionGatingInterruptionOutcome] that describes the outcome of the interruption.
     */
    fun interrupt(performSessionSetup: () -> Unit): DragAndDropSessionGatingInterruptionOutcome {
        val gatedGesturesRecognizers = checkNotNull(gatedGestureRecognizers) {
            "DragAndDropSessionGatingGestureRecognizer.interrupt() called before configuration"
        }

        if (state != UIGestureRecognizerStatePossible) {
            // Failure requirements can not be met if this gesture recognizer is not in the possible
            // state. It's either already failed and sits in cancelled state, or running, so the
            // drag and drop session can't start anyway.
            return DragAndDropSessionGatingInterruptionOutcome.IMPOSSIBLE
        }

        val areAllGatedGesturesPossible = gatedGesturesRecognizers.all {
            if (it.state == UIGestureRecognizerStatePossible) {
                true
            } else {
                println("State of this gesture recognizer: ${this.state.asUIGestureRecognizerStateString()}")
                println("areAllGatedGesturesPossible: false, because $it is in ${it.state.asUIGestureRecognizerStateString()} state")
                false
            }
        }

        if (areAllGatedGesturesPossible) {
            performSessionSetup()
        }

        setState(UIGestureRecognizerStateFailed)

        return if (areAllGatedGesturesPossible) {
            DragAndDropSessionGatingInterruptionOutcome.POTENTIAL_SUCCESS
        } else {
            DragAndDropSessionGatingInterruptionOutcome.REDUNDANT_GATING
        }
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleOtherGestureRecognizer(gestureRecognizer: UILongPressGestureRecognizer) {
        println("handleOtherGestureRecognizer: $gestureRecognizer")
    }

    /**
     * Fails the gated gesture recognisers immediately by setting the state of this gesture recognizer
     * to [UIGestureRecognizerStateBegan]. They won't be able to start the drag and drop
     * session until the next gesture sequence.
     */
    fun failGatedGestureRecognisers() {
        if (state == UIGestureRecognizerStatePossible && numberOfTouches > 0UL) {
            setState(UIGestureRecognizerStateBegan)
        }
    }

    override fun gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        require(gestureRecognizer == this) {
            "Unexpected gesture recognizer $gestureRecognizer delegated to $this.c"
        }

        val gatedGestureRecognizers = checkNotNull(gatedGestureRecognizers) {
            "`UIGestureRecognizerDelegate` methods called before `DragGatingGestureRecognizer.configure`"
        }

        val view = requireNotNull(view) {
            "$this is not attached to a view"
        }
        val otherView = requireNotNull(otherGestureRecognizer.view) {
            "$otherGestureRecognizer is not attached to a view"
        }

        return if (view == otherView) {
            // We allow simultaneous recognition with other gesture recognizers to the same view
            // and are not in the gated gesture recognizers set
            val result = otherGestureRecognizer !in gatedGestureRecognizers
            if (result) {
                println("${this.className} allows ${otherGestureRecognizer.className} to recognize simultaneously")
            } else {
                println("${this.className} does not allow ${otherGestureRecognizer.className} to recognize simultaneously")
            }
            result
        } else {
            // and all other gesture recognizers
            println("${this.className} allows ${otherGestureRecognizer.className} to recognize simultaneously")
            true
        }
    }

    override fun gestureRecognizerShouldBeRequiredToFailByGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        require(gestureRecognizer == this) {
            "Unexpected ${gestureRecognizer.className} delegated to ${this.className}"
        }

        val gatedGestureRecognizers = checkNotNull(gatedGestureRecognizers) {
            "`UIGestureRecognizerDelegate` methods called before `DragGatingGestureRecognizer.configure`"
        }

        return if (gatedGestureRecognizers.contains(otherGestureRecognizer)) {
            // We require the gated gestures to wait for this gesture recognizer to fail
            println("${this.className} is required to fail by ${otherGestureRecognizer.className}")
            true
        } else {
            // We don't require other gesture recognizers to wait for this gesture recognizer to fail
            println("${this.className} is not required to fail by ${otherGestureRecognizer.className}")
            false
        }
    }

    override fun gestureRecognizerShouldRequireFailureOfGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        require(gestureRecognizer == this) {
            "Unexpected  ${gestureRecognizer.className} delegated to ${this.className}"
        }

        checkNotNull(gatedGestureRecognizers) {
            "`UIGestureRecognizerDelegate` methods called before `DragGatingGestureRecognizer.configure`"
        }

        // We don't require other gesture recognizers to fail
        println("${this.className} does not require ${otherGestureRecognizer.className} to fail")
        return false
    }

    /**
     * Adds this gesture recognizer to the given [view] and sets up the required failure
     * requirements for the [UILongPressGestureRecognizer]s that assumed to be backing the
     * [UIDragInteraction] logic in the view.
     */
    fun configure(view: UIView, gestureRecognizersRequiredToFail: HashSet<UIGestureRecognizer>) {
        check(gatedGestureRecognizers == null) {
            "DragAndDropSessionGatingGestureRecognizer.configure() called multiple times"
        }

        if (gestureRecognizersRequiredToFail.isEmpty()) {
            println("Warning: DragAndDropSessionGatingGestureRecognizer is configured with no gated gesture recognizers")
        }

        view.addGestureRecognizer(this)
        gatedGestureRecognizers = gestureRecognizersRequiredToFail

        for (gestureRecognizer in gestureRecognizersRequiredToFail) {
            gestureRecognizer.addTarget(this, NSSelectorFromString(::handleOtherGestureRecognizer.name + ":"))

            if (gestureRecognizer is UILongPressGestureRecognizer) {
                // override the minimum press duration to 0.0 to allow the drag and drop to start
                // immediately when we fail this gesture recognizer and not rely on the press duration
                // of Compose internal longPressGesture
                gestureRecognizer.minimumPressDuration = 0.0

            }
        }
    }
}