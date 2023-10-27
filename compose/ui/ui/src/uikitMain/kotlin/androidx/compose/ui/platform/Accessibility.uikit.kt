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
import platform.Foundation.NSNotFound
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSObject
import androidx.compose.objc.UIAccessibilityContainerWorkaroundProtocol
import kotlin.test.todo
import platform.UIKit.UIView
import platform.darwin.NSInteger

private fun <R> debugPrint(name: String, block: () -> R): R {
    val value = block()
    println("$name: $value")
    return value
}

fun NSObject.fillAccessibilityProperties(semanticsNode: SemanticsNode) {
    semanticsNode.config.forEach {
        when (it.key) {
            else -> {}
        }
    }
}

private class ComposeAccessibilityElement(
    val controller: AccessibilityControllerImpl,
    val semanticsNode: SemanticsNode,
    parent: Any,
) : UIAccessibilityElement(parent) {
    init {
        accessibilityIdentifier = "Element for ${semanticsNode.id}"

        if (semanticsNode.children.size == 0) {
            accessibilityLabel = "SemanticsNode ID = ${semanticsNode.id}"
        }

        accessibilityFrame = controller.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)
        fillAccessibilityProperties(semanticsNode)
    }
}

/**
 * UIAccessibilityElement can't be a container and an element at the same time.
 * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
 * UIAccessibilityContainer methods.
 * Thus, semantics tree like
 * ```
 * SemanticsNode_A
 *     SemanticsNode_B
 *         SemanticsNode_C
 * ```
 * Will be represented like:
 * ```
 * ComposeAccessibilityContainer_A
 *     ComposeAccessibilityElement_A
 *     ComposeAccessibilityContainer_B
 *         ComposeAccessibilityElement_B
 *         ComposeAccessibilityElement_C
 * ```
 */
private class ComposeAccessibilityContainer(
    controller: AccessibilityControllerImpl,
    semanticsNode: SemanticsNode,
    parent: Any,
) : UIAccessibilityElement(parent),
    UIAccessibilityContainerWorkaroundProtocol {
    private val children: List<Any>
    private val wrappedElement = ComposeAccessibilityElement(controller, semanticsNode, this)

    init {
        isAccessibilityElement = false
        accessibilityIdentifier = "Container for ${semanticsNode.id}"
        accessibilityFrame = wrappedElement.accessibilityFrame
        accessibilityContainer = parent

        children = wrappedElement.semanticsNode.replacedChildren.map {
            createComposeAccessibleObject(wrappedElement.controller, it, this)
        }
    }

    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        val idx = index.toInt()

        if (idx < 0 || idx >= accessibilityElementCount().toInt()) {
            return null
        }

        if (idx == 0) {
            return wrappedElement
        }

        return children[idx - 1]
    }

    override fun accessibilityElementCount(): NSInteger = (children.size + 1).toLong()

    override fun indexOfAccessibilityElement(element: Any?): NSInteger {
        // TODO: store the elements in Int->Any map, if that lookup takes significant time
        if (element == null) {
            return NSNotFound
        }

        if (element == wrappedElement) {
            return 0
        }

        val index = children.indexOf(element)

        return if (index == -1) {
            NSNotFound
        } else {
            (index + 1).toLong()
        }
    }
}

private fun createComposeAccessibleObject(
    controller: AccessibilityControllerImpl,
    semanticsNode: SemanticsNode,
    parent: Any
): Any {
    return if (semanticsNode.children.size == 0) {
        ComposeAccessibilityElement(controller, semanticsNode, parent)
    } else {
        ComposeAccessibilityContainer(controller, semanticsNode, parent)
    }
}

internal class AccessibilityControllerImpl(
    val rootAccessibleContainer: UIView,
    val owner: SemanticsOwner,
    val convertRectToWindowSpaceCGRect: (Rect) -> CValue<CGRect>
) : AccessibilityController {
    /**
     * Represents the current state of the [ComposeAccessibleTemp] tree cleanliness.
     *
     * A value of true indicates that the Compose accessible tree is dirty, meaning that compose semantics tree was modified since last sync,
     * false otherwise.
     */
    private var isCurrentComposeAccessibleTreeDirty = false

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

    private fun syncNodes() {
        val rooSemanticstNode = owner.rootSemanticsNode

        if (!rooSemanticstNode.layoutNode.isPlaced) {
            return
        }

        if (!isCurrentComposeAccessibleTreeDirty) {
            return
        }

        isCurrentComposeAccessibleTreeDirty = false

        val accessibilityElements = rooSemanticstNode.replacedChildren
            .reversed()
            .map {
                createComposeAccessibleObject(this, it, rootAccessibleContainer)
            }

        fun traverse(any: Any, depth: Int = 0) {
            fun gap(): String =
                "  ".repeat(depth)

            println("${gap()} $any")

            if (any is UIAccessibilityContainerWorkaroundProtocol) {
                for (i in 0..any.accessibilityElementCount() - 1) {
                    any.accessibilityElementAtIndex(i.toLong())?.let {
                        traverse(it, depth + 1)
                    }
                }
            }
        }

        accessibilityElements.forEach { traverse(it) }

        rootAccessibleContainer.accessibilityElements = accessibilityElements
    }
}