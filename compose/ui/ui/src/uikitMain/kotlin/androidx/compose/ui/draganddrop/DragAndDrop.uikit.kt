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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.interop.LocalUIKitInteropContainer
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGPoint
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropInteractionDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.addInteraction
import platform.UIKit.removeInteraction
import platform.UIKit.UIEvent
import platform.darwin.NSObject

private class CupertinoDragAndDropNode(
    container: InteropContainer<UIView>,
    dragInteractionDelegate: UIDragInteractionDelegateProtocol?,
    dropInteractionDelegate: UIDropInteractionDelegateProtocol?,
) : TrackInteropModifierNode<UIView>(
    container = container,
    nativeView = UIView().also {
        it.setUserInteractionEnabled(true)
    }
) {
    private var dragInteraction: UIDragInteraction? = null
    var dragInteractionDelegate = dragInteractionDelegate
        set(value) {
            field = value
            updateDragInteraction()
        }

    private var dropInteraction: UIDropInteraction? = null
    var dropInteractionDelegate = dropInteractionDelegate
        set(value) {
            field = value
            updateDropInteraction()
        }

    init {
        updateDragInteraction()
    }

    private fun updateDragInteraction() {
        val view = nativeView ?: return

        // Remove existing interaction if any
        dragInteraction?.let { view.removeInteraction(it) }

        dragInteractionDelegate?.let { delegate ->
            // Store new interaction and add it to the view
            dragInteraction = UIDragInteraction(delegate = delegate)
                .also { interaction ->
                    view.addInteraction(interaction)
                }
        }
    }

    private fun updateDropInteraction() {
        val view = nativeView ?: return

        // Remove existing interaction if any
        dropInteraction?.let { view.removeInteraction(it) }

        dropInteractionDelegate?.let { delegate ->
            // Store new interaction and add it to the view
            dropInteraction = UIDropInteraction(delegate = delegate)
                .also { interaction ->
                    view.addInteraction(interaction)
                }
        }
    }
}

private data class CupertinoDragAndDropElement(
    val container: InteropContainer<UIView>,
    val dragInteractionDelegate: UIDragInteractionDelegateProtocol?,
    val dropInteractionDelegate: UIDropInteractionDelegateProtocol?,
) : ModifierNodeElement<CupertinoDragAndDropNode>() {
    override fun create() = CupertinoDragAndDropNode(
        container = container,
        dragInteractionDelegate = dragInteractionDelegate,
        dropInteractionDelegate = dropInteractionDelegate
    )

    override fun update(node: CupertinoDragAndDropNode) {
        node.container = container
        node.dragInteractionDelegate = dragInteractionDelegate
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cupertinoDragSource"
    }
}

@ExperimentalComposeUiApi
interface CupertinoDragSource {
    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2891010-draginteraction
     */
    fun itemsForBeginning(session: UIDragSessionProtocol): List<UIDragItem> = emptyList()
}

@ExperimentalComposeUiApi
interface CupertinoDropTarget {

}

/**
 * Modifier that adds a drag and drop dummy view hovering above the element.
 * On iOS, unlike other platforms, drag-and-drop sessions can only be triggered by internal API of
 * native views via [UIDragInteraction]. It's not allowed to start a drag-and-drop session imperatively
 * and attach it to the context of current [UIEvent].
 *
 * Related documentation:
 * [UIDragInteractionDelegate](https://developer.apple.com/documentation/uikit/uidraginteractiondelegate)
 * [UIDragInteractionDelegate](https://developer.apple.com/documentation/uikit/uidropinteractiondelegate)
 *
 */
@ExperimentalComposeUiApi
fun Modifier.dragAndDrop(
    dragInteractionDelegate: UIDragInteractionDelegateProtocol? = null,
    dropInteractionDelegate: UIDropInteractionDelegateProtocol? = null
): Modifier = composed {
    val interopContainer = LocalUIKitInteropContainer.current

    CupertinoDragAndDropElement(
        container = interopContainer,
        dragInteractionDelegate = dragInteractionDelegate,
        dropInteractionDelegate = dropInteractionDelegate
    )
}
