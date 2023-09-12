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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLock

internal sealed class UIKitInteropAction

internal data class UIKitInteropArbitaryAction(val block: () -> Unit) : UIKitInteropAction() {
    fun invoke() = block()
}

internal enum class UIKitInteropState {
    BEGAN, ENDED
}

internal data class UIKitInteropStateUpdate(val state: UIKitInteropState) : UIKitInteropAction()

internal enum class UIKitInteropViewHierarchyChange {
    VIEW_ADDED,
    VIEW_REMOVED
}

/**
 * Class which can be used to add actions related to UIKit objects to be executed in sync with compose rendering,
 * Addding deferred actions is threadsafe, but they will be executed in the order of their submission, and on the main thread.
 */
internal class UIKitInteropContext(
    val requestRedraw: () -> Unit
) {
    private val lock: NSLock = NSLock()
    private val actions = mutableListOf<UIKitInteropAction>()

    /**
     * Number of views, created by interop API and present in current view hierarchy
     */
    private var viewsCount = 0
        set(value) {
            require(value >= 0)

            field = value
        }

    /**
     * Add lambda to a list of commands which will be executed later in the same CATransaction, when the next rendered Compose frame is presented
     */
    fun deferAction(hierarchyChange: UIKitInteropViewHierarchyChange? = null, action: () -> Unit) {
        requestRedraw()

        lock.doLocked {
            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_ADDED) {
                if (viewsCount == 0) {
                    actions.add(UIKitInteropStateUpdate(UIKitInteropState.BEGAN))
                }
                viewsCount += 1
            }

            actions.add(UIKitInteropArbitaryAction(action))

            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                viewsCount -= 1
                if (viewsCount == 0) {
                    actions.add(UIKitInteropStateUpdate(UIKitInteropState.ENDED))
                }
            }
        }
    }

    /**
     * Return a copy of the list of [actions] and clear internal storage.
     */
    internal fun retrieve(): List<UIKitInteropAction> {
        return lock.doLocked {
            val result = actions.toList()
            actions.clear()
            result
        }
    }
}

private inline fun <T> NSLock.doLocked(block: () -> T): T {
    lock()

    try {
        return block()
    } finally {
        unlock()
    }
}

internal val LocalUIKitInteropContext = staticCompositionLocalOf<UIKitInteropContext> {
    error("CompositionLocal UIKitInteropContext not provided")
}