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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.state.ToggleableState
import kotlin.test.todo
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIView
import platform.UIKit.accessibilityCustomActions
import platform.UIKit.accessibilityElementsHidden
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityTextualContext
import platform.UIKit.accessibilityTraits
import platform.UIKit.accessibilityValue
import platform.darwin.NSInteger

private fun <R> debugPrint(name: String, block: () -> R): R {
    val value = block()
    println("$name: $value")
    return value
}

/**
 * Set current object UIAccessibility properties using the [SemanticsNode] properties
 *
 * accessibilityLabel: A string that succinctly identifies the element. It's what a screen reader speaks to describe the element.
 * accessibilityHint: Provides additional context about an element, typically describing what will happen if the user interacts with it. It helps users understand actions associated with the element.
 * accessibilityValue: Conveys the value of an element, such as the current setting of a slider or the text inside a text field.
 * accessibilityTraits: Describes the characteristics of the element, such as being a button, selected, a link, a header, etc. This helps users understand how to interact with the element.
 * accessibilityElementsHidden: When set to true, it hides the element and all its children from the accessibility system. Useful when a widget is present in the hierarchy but covered with other widget.
 * isAccessibilityElement: Determines whether the element should be exposed to the accessibility system.
 * accessibilityFrame: The frame of the element in screen coordinates, helping the accessibility system to know where it is located.
 * accessibilityPath: A path object that defines the shape of the element, used for more precise element description and interaction.
 * accessibilityViewIsModal: Indicates whether interacting with this element requires the user to dismiss a modal view first.
 */
private fun NSObject.fillInAccessibilityProperties(semanticsNode: SemanticsNode) {
    // If the node doesn't have any semantics that can be projected to iOS UIAccessibility entities, it will be invisible to accessibility services
    isAccessibilityElement = false

    var hasAnyMeaningfulSemantics = false

    fun onMeaningfulSemanticAdded() {
        hasAnyMeaningfulSemantics = true
    }

    println(semanticsNode.config)

    val accessibilityLabelStrings = mutableListOf<String>()
    val accessibilityValueStrings = mutableListOf<String>()

    fun addTrait(trait: UIAccessibilityTraits) {
        accessibilityTraits = accessibilityTraits or trait
    }

    fun <T>getValue(key: SemanticsPropertyKey<T>): T = semanticsNode.config[key]

    // Iterate through all semantic properties and map them to values that are expected by iOS Accessibility services for the node with given semantics
    semanticsNode.config.forEach { pair ->
        when (val key = pair.key) {
            // == Properties ==

            SemanticsProperties.InvisibleToUser -> {
                // Return immediately. Function won't reach a point where [isAccessibilityElement] is set to true
                return
            }

            SemanticsProperties.LiveRegion -> {
                onMeaningfulSemanticAdded()
                addTrait(UIAccessibilityTraitUpdatesFrequently)
            }

            SemanticsProperties.ContentDescription -> {
                accessibilityLabelStrings.addAll(getValue(key))
            }

            SemanticsProperties.Text -> {
                accessibilityLabelStrings.addAll(getValue(key).map { it.text })
            }

            SemanticsProperties.PaneTitle -> {
                accessibilityLabelStrings.add(getValue(key))
            }

            SemanticsProperties.Disabled -> {
                addTrait(UIAccessibilityTraitNotEnabled)
            }

            SemanticsProperties.Heading -> {
                onMeaningfulSemanticAdded()
                addTrait(UIAccessibilityTraitHeader)
            }

            SemanticsProperties.StateDescription -> {
                val state = getValue(key)
                accessibilityValueStrings.add(state)
            }

            SemanticsProperties.ToggleableState -> {
                val state = getValue(key)

                when (state) {
                    ToggleableState.On -> {
                        addTrait(UIAccessibilityTraitSelected)
                        accessibilityValueStrings.add("On")
                    }

                    ToggleableState.Off -> {
                        accessibilityValueStrings.add("Off")
                    }

                    ToggleableState.Indeterminate -> {
                        accessibilityValueStrings.add("Indeterminate")
                    }
                }
            }

            SemanticsProperties.Role -> {
                val role = getValue(key)

                when (role) {
                    Role.Button, Role.RadioButton, Role.Checkbox, Role.Switch -> {
                        onMeaningfulSemanticAdded()
                        addTrait(UIAccessibilityTraitButton)
                    }

                    Role.DropdownList -> {
                        onMeaningfulSemanticAdded()
                        addTrait(UIAccessibilityTraitAdjustable)
                    }

                    Role.Image -> {
                        onMeaningfulSemanticAdded()
                        addTrait(UIAccessibilityTraitImage)
                    }
                }
            }

            // == Actions ==

            SemanticsActions.OnClick -> {
                onMeaningfulSemanticAdded()
            }

            SemanticsActions.CustomActions -> {
                onMeaningfulSemanticAdded()

                val actions = getValue(key)
                accessibilityCustomActions = actions.map {
                    UIAccessibilityCustomAction(
                        name = it.label,
                        actionHandler = { _ ->
                            it.action.invoke()
                        }
                    )
                }
            }
        }
    }

    if (accessibilityLabelStrings.isNotEmpty()) {
        onMeaningfulSemanticAdded()
        accessibilityLabel = accessibilityLabelStrings.joinToString("\n") { it }
    }

    if (accessibilityValueStrings.isNotEmpty()) {
        onMeaningfulSemanticAdded()
        accessibilityValue = accessibilityLabelStrings.joinToString("\n") { it }
    }

    isAccessibilityElement = hasAnyMeaningfulSemantics
}

private class ComposeAccessibilityElement(
    val controller: AccessibilityControllerImpl,
    val semanticsNode: SemanticsNode,
    parent: Any,
) : UIAccessibilityElement(parent) {
    init {
        accessibilityIdentifier = "Element for ${semanticsNode.id}"
        accessibilityFrame = controller.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)

        fillInAccessibilityProperties(semanticsNode)
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
        // TODO: store the elements in Any->Int map, if that lookup takes significant time
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
    return if (semanticsNode.children.isEmpty()) {
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
     * Represents the current tree cleanliness.
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
        // Copied from desktop implementation
        while (true) {
            syncNodes()
            delay(100)
        }
    }

    private fun syncNodes() {
        val rooSemanticstNode = owner.rootSemanticsNode

        // TODO: the entire NSObject tree is eagerly recreated now when semantics are invalidated. This is not optimal.

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
                for (i in 0 until any.accessibilityElementCount()) {
                    any.accessibilityElementAtIndex(i)?.let {
                        traverse(it, depth + 1)
                    }
                }
            }
        }

        accessibilityElements.forEach { traverse(it) }

        rootAccessibleContainer.accessibilityElements = accessibilityElements
    }
}