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
import platform.UIKit.UIEvent
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGestureRecognizerStatePossible
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.setState

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
     * The drag and drop session is not possible
     */
    FAILURE
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
    private var gatedGestureRecognizers: List<UIGestureRecognizer>? = null

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        // no-op
    }

    /**
     * Mark this gesture recognizer as failed to allow native [UILongPressGestureRecognizer]s responsible
     * for [UIDragInteraction] to start.
     *
     * @return true if the gesture recognizer was marked as failed, false otherwise.
     */
    fun interrupt(): DragAndDropSessionGatingInterruptionOutcome {
        val gatedGesturesRecognizers = checkNotNull(gatedGestureRecognizers) {
            "DragAndDropSessionGatingGestureRecognizer.interrupt() called before configuration"
        }

        val areAllGatedGestureRecognizersPossible = gatedGesturesRecognizers.all {
            it.state == UIGestureRecognizerStatePossible
        }

        setState(UIGestureRecognizerStateFailed)

        return if (areAllGatedGestureRecognizersPossible) {
            DragAndDropSessionGatingInterruptionOutcome.POTENTIAL_SUCCESS
        } else {
            DragAndDropSessionGatingInterruptionOutcome.FAILURE
        }
    }

    /**
     * Adds this gesture recognizer to the given [view] and sets up the required failure
     * requirements for the [UILongPressGestureRecognizer]s that assumed to be backing the
     * [UIDragInteraction] logic in the view.
     */
    fun configure(view: UIView, gestureRecognizersRequiredToFail: List<UIGestureRecognizer>) {
        check(gatedGestureRecognizers == null) {
            "DragAndDropSessionGatingGestureRecognizer.configure() called multiple times"
        }

        view.addGestureRecognizer(this)

        for (gestureRecognizer in gestureRecognizersRequiredToFail) {
            gestureRecognizer.requireGestureRecognizerToFail(this)

            if (gestureRecognizer is UILongPressGestureRecognizer) {
                // override the minimum press duration to 0.0 to allow the drag and drop to start
                // immediately when we fail this gesture recognizer and not rely on the press duration
                // of Compose internal longPressGesture
                gestureRecognizer.minimumPressDuration = 0.0

            }
        }
    }
}