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

import androidx.collection.ArraySet
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal class DragAndDropManagerImpl : DragAndDropManager {
    private val rootDragAndDropNode = DragAndDropNode { null }

    private val interestedNodes = ArraySet<DragAndDropModifierNode>()

    override val modifier: Modifier = object : ModifierNodeElement<DragAndDropNode>() {
        override fun create() = rootDragAndDropNode

        override fun update(node: DragAndDropNode) = Unit

        override fun InspectorInfo.inspectableProperties() {
            name = "RootDragAndDropNode"
        }

        override fun hashCode(): Int = rootDragAndDropNode.hashCode()

        override fun equals(other: Any?) = other === this
    }

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean =
        true

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }
}