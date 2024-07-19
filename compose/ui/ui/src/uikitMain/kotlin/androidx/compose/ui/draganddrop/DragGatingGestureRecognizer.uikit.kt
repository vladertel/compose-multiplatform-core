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
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.setState

/**
 * This class is a dummy gesture recognizer that is used to gate drag and drop events.
 * It is used to configure failure requirements for [UIGestureRecognizer]s created by
 * [UIDragInteraction].
 *
 * Until this fails explicitly, the drag and drop session can not be started by the system.
 */

@OptIn(ExperimentalObjCName::class)
@ObjCName("DragGatingGestureRecognizer")
internal class DragGatingGestureRecognizer: CMPGestureRecognizer(target = null, action = null) {
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
     */
    fun allowDragAndDrop() {
        setState(UIGestureRecognizerStateFailed)
    }

    /**
     * Adds this gesture recognizer to the given [view] and sets up the required failure
     * requirements for the [UILongPressGestureRecognizer]s that assumed to be backing the
     * [UIDragInteraction] logic in the view.
     */
    fun setupInView(view: UIView) {
        view.addGestureRecognizer(this)

        view.gestureRecognizers?.forEach {
            // This is fragile code and can potentially break if we bring our own [UILongPressGestureRecognizer]
            // or if Apple changes the implementation of [UIDragInteraction] in the future.
            // We are relying on the fact that [UIDragInteraction] creates a [UILongPressGestureRecognizer]
            // If something breaks, we will need to revisit this code.

            if (it is UILongPressGestureRecognizer) {
                // override the minimum press duration to 0.0 to allow the drag and drop to start
                // immediately when we fail this gesture recognizer
                it.minimumPressDuration = 0.0
                it.requireGestureRecognizerToFail(this)
            }
        }
    }
}