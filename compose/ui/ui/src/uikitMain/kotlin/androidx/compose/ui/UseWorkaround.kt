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

package androidx.compose.ui

import platform.UIKit.UIAccessibilityElement
import platform.darwin.NSInteger
import platform.darwin.NSObject

class UseWorkaround(container: Any) : UIAccessibilityElement(container),
    androidx.compose.objc.UIAccessibilityContainerWorkaroundProtocol {
        init {
            isAccessibilityElement = false
        }

    override fun accessibilityElementAtIndex(index: NSInteger /* = Long */): Any? {
        println("call accessibilityElementAtIndex")
        TODO()
    }

    override fun accessibilityElementCount(): NSInteger {
        println("call accessibilityElementCount")
        return 0L
    }

    override fun indexOfAccessibilityElement(element: Any?): NSInteger {
        println("call indexOfAccessibilityElement")
        return 0L
    }

}
