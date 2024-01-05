/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectRepeatingTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput

internal actual fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean =
    false //TODO implement copy key event for iPad

internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier =
    this //TODO support magnifier for uikit

internal actual fun Modifier.additionalTouchSelectionModifier(manager: SelectionManager, releaseAction: () -> Unit): Modifier {
    /**
     * Detect tap without consuming the up event.
     */ //it was private method in SelectionManager, moved here to support different behavior for other platforms
    suspend fun PointerInputScope.detectNonConsumingTap(onTap: (Offset) -> Unit) {
        awaitEachGesture {
            waitForUpOrCancellation()?.let {
                onTap(it.position)
            }
        }
    }

//    suspend fun PointerInputScope.detectRepeatingTaps(onTap: (Offset) -> Unit) {
//        awaitEachGesture {
//            val touchesCounter = ClicksCounter(viewConfiguration, clicksSlop = 50.dp.toPx())
//            val touchEvent = awaitPointerEvent(PointerEventPass.Main)
//            touchesCounter.update(touchEvent.changes[0])
//
//        }
//    }

//    suspend fun AwaitPointerEventScope.awaitFirstDown(
//        requireUnconsumed: Boolean = true,
//        pass: PointerEventPass = PointerEventPass.Main,
//    ): PointerInputChange {
//        var event: PointerEvent
//        do {
//            event = awaitPointerEvent(pass)
//        } while (!event.isPrimaryChangedDown(requireUnconsumed))
//        return event.changes[0]
//    }

//    /**
//     * Reads events in the given [pass] until all pointers are up or the gesture was canceled.
//     * The gesture is considered canceled when a pointer leaves the event region, a position
//     * change has been consumed or a pointer down change event was already consumed in the given
//     * pass. If the gesture was not canceled, the final up change is returned or `null` if the
//     * event was canceled.
//     */
//    suspend fun AwaitPointerEventScope.waitForUpOrCancellation(
//        pass: PointerEventPass = PointerEventPass.Main
//    ): PointerInputChange? {
//        while (true) {
//            val event = awaitPointerEvent(pass)
//            if (event.changes.fastAll { it.changedToUp() }) {
//                // All pointers are up
//                return event.changes[0]
//            }
//
//            if (event.changes.fastAny {
//                    it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
//                }
//            ) {
//                return null // Canceled
//            }
//
//            // Check for cancel by position consumption. We can look on the Final pass of the
//            // existing pointer event because it comes after the pass we checked above.
//            val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
//            if (consumeCheck.changes.fastAny { it.isConsumed }) {
//                return null
//            }
//        }
//    }

    return if (manager.hasFocus) pointerInput(Unit) {
        detectRepeatingTapGestures(onTap = { releaseAction() }, onDoubleTap = {
            manager.focusRequester.requestFocus()
            manager.updateSelection(newPosition = it, previousPosition = it, isStartHandle = true, adjustment = SelectionAdjustment.Word)
        })
    } else this
}