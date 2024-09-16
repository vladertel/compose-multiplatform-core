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

package androidx.compose.ui.test

import androidx.compose.ui.uikit.utils._setIsFirstTouchForView
import androidx.compose.ui.uikit.utils._setLocationInWindow
import androidx.compose.ui.uikit.utils.setPhase
import androidx.compose.ui.uikit.utils.setTapCount
import androidx.compose.ui.uikit.utils.setTimestamp
import androidx.compose.ui.uikit.utils.setView
import androidx.compose.ui.uikit.utils.setWindow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.asCGPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
internal class SyntheticTouch(
    view: UIView,
    location: DpOffset,
): UITouch() {
    init {
        val window = view.window ?: error("Can't initialize touch in detached view")

        setWindow(window)
        setTapCount(1u)
        _setLocationInWindow(location.asCGPoint(), resetPrevious = true)

        val hitTestView = window.hitTest(location.asCGPoint(), withEvent = null)

        setView(hitTestView)
        setPhase(UITouchPhase.UITouchPhaseBegan)

        // TODO: Check it:
        _setIsFirstTouchForView(true)
        // if ([self respondsToSelector:@selector(_setIsFirstTouchForView:)]) {
        //     [self _setIsFirstTouchForView:YES];
        // } else {
        //     [self _setIsTapToClick:YES];
        //     // We modify the touchFlags ivar struct directly.
        //     // First entry is _firstTouchForView
        //     Ivar flagsIvar = class_getInstanceVariable(object_getClass(self), "_touchFlags");
        //     ptrdiff_t touchFlagsOffset = ivar_getOffset(flagsIvar);
        //     char *flags = (__bridge void *)self + touchFlagsOffset;
        //     *flags = *flags | (char)0x01;
        // }

        setTimestamp(NSProcessInfo.processInfo.systemUptime)

        // TODO: Check it:
        // setGestureView(hitTestView)
        // if ([self respondsToSelector:@selector(setGestureView:)]) {
        //     [self setGestureView:hitTestView];
        // }

        // val event = kif_IOHIDEventWithTouches(listOf(this))
        // _setHidEvent(event)
    }

    fun setPhaseAndUpdateTimestamp(phase: UITouchPhase) {
        setTimestamp(NSProcessInfo.processInfo.systemUptime)
        setPhase(phase)
    }
}
