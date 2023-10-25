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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRect
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSObject

private class ComposeAccessibleElement: UIAccessibilityElement() {
    /**
     * NSObject containing information which is fed to iOS Accessibility Services to allow user interact with the app
     */
    val accessibilityObject: NSObject
        get() = this

    var id = 0
    var parent: ComposeAccessibleElement? = null
    var hasChildren = false
    val children = mutableListOf<ComposeAccessibleElement>()
    var semanticsNode: SemanticsNode? = null
}

/**
 * UIAccessibilityElement can't be a container and an element at the same time.
 * Thus, semantics tree like
 * + SemanticsNode_A
 * | + SemanticsNode_B
 * | | -SemanticsNode_C
 *
 * Will be represented like:
 * + ComposeAccessibleContainer_A
 * | - ComposeAccessibleElement_A
 * | + ComposeAccessibleContainer_B
 * | | + ComposeAccessibleElement_B
 * | | - ComposeAccessibleElement_C
 *
 */
private class ComposeAccessibleContainer(
    val wrappedElement: ComposeAccessibleElement
): UIAccessibilityElement() {
}

/**
 * NSObject used as a node in the tree to be used as a data source for iOS accessibility services.
 */
internal class ComposeAccessibleTemp(
    container: Any,
    node: SemanticsNode,
    val convertRectToWindowSpaceCGRect: (Rect) -> CValue<CGRect>
): UIAccessibilityElement(container) {
    init {
        isAccessibilityElement = true

        val text = node.config.getOrNull(SemanticsProperties.Text)
        if (text != null) {
            accessibilityLabel = text.joinToString {
                it.text
            }
        }
        //accessibilityLabel = "${node.id}"
        accessibilityFrame = convertRectToWindowSpaceCGRect(node.boundsInWindow)
    }
}

internal class AccessibilityControllerImpl(
    val rootAccessibleContainer: NSObject,
    val owner: SemanticsOwner,
    val convertRectToWindowSpaceCGRect: (Rect) -> CValue<CGRect>
): AccessibilityController {
    /**
     * Represents the current state of the [ComposeAccessibleTemp] tree cleanliness.
     *
     * A value of true indicates that the Compose accessible tree is dirty, meaning that compose semantics tree was modified since last sync,
     * false otherwise.
     */
    private var isCurrentComposeAccessibleTreeDirty = false

    /**
     * Cache of current [ComposeAccessibleTemp] objects. The key is [SemanticsNode.id] of corresponding [SemanticsNode].
     */
    private val composeAccessibleMap = mutableMapOf<Int, ComposeAccessibleTemp>()

    /**
     * Represent a set of [SemanticsNode.id] that are currently in the tree.
     */
    private val inUseIds = mutableSetOf<Int>()

    override fun onSemanticsChange() {
        isCurrentComposeAccessibleTreeDirty = true
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
    }

    override suspend fun syncLoop() {
        while (true) {
            syncNodes()
            delay(100)
        }
    }

    /**
     * Recursively traverses the [SemanticsNode] tree and invokes [block] with every node except the root one
     */
    private fun traverseSemanticsTree(block: (SemanticsNode) -> Unit) {
        val rootNode = owner.rootSemanticsNode

        rootNode.children.forEach {
            traverseSemanticsTree(it, block)
        }
    }

    /**
     * Apply [block] to [SemanticsNode] and all its children recursively
     */
    private fun traverseSemanticsTree(node: SemanticsNode, block: (SemanticsNode) -> Unit) {
        block(node)

        node.children.forEach {
            traverseSemanticsTree(it, block)
        }
    }

    private fun syncNodes() {
        val rootNode = owner.rootSemanticsNode

        if (!rootNode.layoutNode.isPlaced) {
            return
        }

        if (!isCurrentComposeAccessibleTreeDirty) {
            return
        }

        isCurrentComposeAccessibleTreeDirty = false

        inUseIds.clear()

        traverseSemanticsTree {
            inUseIds.add(it.id)

            if (!composeAccessibleMap.containsKey(it.id)) {
                composeAccessibleMap[it.id] = ComposeAccessibleTemp(rootAccessibleContainer, it, convertRectToWindowSpaceCGRect)
            }
        }

        composeAccessibleMap.entries.retainAll {
            inUseIds.contains(it.key)
        }

        var allNodes = mutableListOf<Any>()
        traverseSemanticsTree {
            val accessible = checkNotNull(composeAccessibleMap[it.id])
            println(it.config)
            allNodes.add(accessible)
        }

//        rootAccessibleContainer.accessibilityElements = owner.rootSemanticsNode.children.map {
//            val accessible = composeAccessibleMap[it.id]
//            println(it)
//            checkNotNull(accessible)
//        }
        rootAccessibleContainer.accessibilityElements = allNodes

        println(rootAccessibleContainer)
    }
}