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

package androidx.compose.ui

import kotlin.js.JsName

// It is usually used when we need to update the current position of the pointers.
// For example, when we relayout or scroll Compose content, it will request update via needSendMove.
// The update happen after relayout via sending a synthetic Move event through all UI tree
internal class PointerPositionUpdater(
    private val onNeedUpdate: () -> Unit,
    private val syntheticEventSender: SyntheticEventSender,
) {
    var needUpdate: Boolean = false
        private set

    fun reset() {
        needUpdate = false
    }

    @JsName("setNeedUpdate")
    fun needSendMove() {
        println("needSendMove")
        needUpdate = true
        onNeedUpdate()
    }

    fun update() {
        println("update $needUpdate")
        if (needUpdate) {
            needUpdate = false
            syntheticEventSender.sendPreviousMove()
        }
    }
}
