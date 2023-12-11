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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotFound
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.toCGRect
import androidx.compose.ui.uikit.utils.*
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIView
import platform.UIKit.accessibilityCustomActions
import platform.darwin.NSInteger

private class AccessibilityElement(
    private val semanticsNode: SemanticsNode,
    private val controller: AccessibilityMediator,
) : CMPAccessibilityElement(controller.view) {
    val semanticsNodeId: Int
        get() = semanticsNode.id

    val hasChildren: Boolean
        get() = children.isNotEmpty()

    val childrenCount: NSInteger
        get() = children.size.toLong()


    val actualAccessibilityElement: Any
        get() = this

    var parent: AccessibilityElement? = null
        private set

    private var children = mutableListOf<AccessibilityElement>()

    /**
     * Constructed lazily if :
     * - The element has children of its own
     * or
     * - The element is representing the root node
     */
    private val synthesizedAccessibilityContainer by lazy {
        AccessibilityContainer(
            element = this,
            controller = controller
        )
    }

    init {
        fillInAccessibilityProperties()
    }

    fun childAtIndex(index: NSInteger): AccessibilityElement? {
        if (index < 0L || index >= childrenCount) {
            return null
        }

        return children[index.toInt()]
    }

    /**
     * Tries to match the given [element] with the actual hierarchy resolution callback from
     * iOS Accessibility services. If the element is found, returns its index in the children list.
     * Otherwise, returns null.
     */
    fun indexOfChildAccessibilityElement(element: Any): NSInteger? {
        for (index in 0 until children.size) {
            val child = children[index]

            // There are two exclusive cases here.
            // 1. The element is a container, and it's the same as the container of the child.
            // 2. The element is an actual accessibility element, and it's the same as one of the child.
            // The first case is true if the child has children itself, and hence [AccessibilityContainer] was communicated to iOS.
            // The second case is true if the child doesn't have children, and hence its [actualAccessibilityElement] was communicated to iOS.

            return if (child.hasChildren) {
                if (element == child.accessibilityContainer) {
                    index.toLong()
                } else {
                    null
                }
            } else {
                if (element == child.actualAccessibilityElement) {
                    index.toLong()
                } else {
                    null
                }
            }
        }

        return null
    }

    override fun accessibilityActivate(): Boolean {
        if (!controller.isAlive) {
            return false
        }

        val onClick = semanticsNode.config.getOrNull(SemanticsActions.OnClick) ?: return false
        val action = onClick.action ?: return false

        return action()
    }

    /**
     * This function is the final one called during the accessibility tree resolution for iOS services
     * and is invoked from underlying Obj-C library. If this node has children, then we return its
     * synthesized container, otherwise we look up the parent and return its container.
     */
    override fun resolveAccessibilityContainer(): Any? {
        if (!controller.isAlive) {
            return null
        }

        return if (hasChildren || semanticsNodeId == controller.rootSemanticsNodeId) {
            synthesizedAccessibilityContainer
        } else {
            parent?.accessibilityContainer
        }
    }

    private fun fillInAccessibilityProperties() {
        // If the node doesn't have any semantics that can be projected to iOS UIAccessibility entities, it will be invisible to accessibility services
        isAccessibilityElement = false

        var hasAnyMeaningfulSemantics = false

        fun onMeaningfulSemanticAdded() {
            hasAnyMeaningfulSemantics = true
        }

        println(semanticsNode.config)

        val accessibilityLabelStrings = mutableListOf<String>()
        val accessibilityValueStrings = mutableListOf<String>()
        var accessibilityTraits = UIAccessibilityTraitNone

        fun addTrait(trait: UIAccessibilityTraits) {
            accessibilityTraits = accessibilityTraits or trait
        }

        fun <T> getValue(key: SemanticsPropertyKey<T>): T = semanticsNode.config[key]

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

        this.accessibilityTraits = accessibilityTraits
        isAccessibilityElement = hasAnyMeaningfulSemantics

        accessibilityIdentifier = "Element for ${semanticsNode.id}"
        accessibilityFrame = controller.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)
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
 * Is expected by iOS Accessibility services to be represented as:
 * ```
 * AccessibilityContainer_A
 *     AccessibilityElement_A -> AccessibilityElement
 *     AccessibilityContainer_B
 *         AccessibilityElement_B -> AccessibilityScrollableElement(for example)
 *         AccessibilityElement_C -> AccessibilityElement
 * ```
 *
 * The actual internal representation of the tree is:
 * ```
 * AccessibilityElement_A
 *   AccessibilityElement_B
 *      AccessibilityElement_C
 * ```
 * But once accessibility services call `UIAcccessibility` API on `AccessibilityElement_A`,
 * this hierarchy will be lazily resolved to the expected one. This is needed, because the actual
 * [SemanticsNode]s can be inserted and removed dynamically, so building it in advance and
 * modifying proactively is not an optimal solution.
 *
 * This implementation is inspired by Flutter's
 * https://github.com/flutter/engine/blob/main/shell/platform/darwin/ios/framework/Source/SemanticsObject.h
 */
private class AccessibilityContainer(
    private val element: AccessibilityElement,
    private val controller: AccessibilityMediator,
) : CMPAccessibilityContainer(controller.view) {
    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        if (index == 0L) {
            return element.actualAccessibilityElement
        }

        val child = element.childAtIndex(index - 1) ?: return null

        if (child.hasChildren) {
            return child.accessibilityContainer
        }

        return child.actualAccessibilityElement
    }

    override fun accessibilityElementCount(): NSInteger = (element.childrenCount + 1)

    /**
     * Reverse lookup of [accessibilityElementAtIndex]
     */
    override fun indexOfAccessibilityElement(element: Any): NSInteger {
        if (element == this.element.actualAccessibilityElement) {
            return 0
        }

        return this.element.indexOfChildAccessibilityElement(element)?.let { index ->
            index + 1
        } ?: NSNotFound
    }

    override fun resolveAccessibilityContainer(): Any? {
        if (!controller.isAlive) {
            return null
        }

        return if (element.semanticsNodeId == controller.rootSemanticsNodeId) {
            controller.view
        } else {
            element.parent?.accessibilityContainer
        }
    }
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
internal class AccessibilityMediator(
    val view: UIView,
    val owner: SemanticsOwner
) {
    // TODO: when is it dead?
    var isAlive = true

    val rootSemanticsNodeId: Int
        get() = owner.rootSemanticsNode.id

    /**
     * A value of true indicates that the Compose accessible tree is dirty, meaning that compose semantics tree was modified since last sync,
     * false otherwise.
     */
    private var isCurrentComposeAccessibleTreeDirty = false

    fun onSemanticsChange() {
        isCurrentComposeAccessibleTreeDirty = true
    }

    fun convertRectToWindowSpaceCGRect(rect: Rect): CValue<CGRect> {
        val window = view.window ?: return CGRectMake(0.0, 0.0, 0.0, 0.0)

        val localSpaceCGRect = rect.toCGRect(window.screen.scale)
        return window.convertRect(localSpaceCGRect, fromView = view)
    }

    suspend fun syncLoop() {
        while (isAlive) {
            syncNodes()
            delay(100)
        }
    }

    fun dispose() {
        isAlive = false

        view.accessibilityElements = null
    }

    private fun traverseSemanticsNode(node: SemanticsNode): AccessibilityElement {
        val accessibilityElement = AccessibilityElement(
            controller = this,
            semanticsNode = node
        )

        for (child in node.replacedChildren) {
            traverseSemanticsNode(child)
        }

        return accessibilityElement
    }

    private fun syncNodes() {
        val rooSemanticstNode = owner.rootSemanticsNode

        // TODO: the entire NSObject tree is eagerly recreated now when semantics are invalidated. This
        //  is not correct behavior
        if (!rooSemanticstNode.layoutNode.isPlaced) {
            return
        }

        if (!isCurrentComposeAccessibleTreeDirty) {
            return
        }

        isCurrentComposeAccessibleTreeDirty = false

        view.accessibilityElements = listOf(
            // Root node will always have synthesized container, look at [AccessibilityElement.resolveAccessibilityContainer]
            traverseSemanticsNode(rooSemanticstNode).resolveAccessibilityContainer()
        )
    }
}

fun List<SemanticsNode>.sortedByAccesibilityOrder(): List<SemanticsNode> {
    // TODO: consider RTL layout
    return sortedBy { it.boundsInWindow.top }
}