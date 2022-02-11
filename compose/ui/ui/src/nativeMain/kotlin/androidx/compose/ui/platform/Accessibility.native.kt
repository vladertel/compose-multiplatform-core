/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.delay

internal class AccessibilityControllerImpl(
    val owner: SkiaBasedOwner,
    val desktopComponent: PlatformComponent
) : AccessibilityController {

    private var currentNodesInvalidated = true

    override suspend fun syncLoop() {
        // TODO: implement native syncLoop
    }
    override fun onLayoutChange(layoutNode: LayoutNode) {
        currentNodesInvalidated = true
    }

    override fun onSemanticsChange() {
        currentNodesInvalidated = true
    }
}
