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

package androidx.compose.ui.window

import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/**
 * UIView which serves as a container for views created by usage of [UIKitView] and [UIKitViewController] API
 */
internal class InteropContainerView : UIView(CGRectZero.readValue()) {
    /**
     * Override `hitTest` to prevent the container from being the target of touch events,
     * effectively making it transparent unless `super.hitTest` returns a descendant view.
     */
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? =
        super.hitTest(point, withEvent).takeIf {
            it != this
        }
}
