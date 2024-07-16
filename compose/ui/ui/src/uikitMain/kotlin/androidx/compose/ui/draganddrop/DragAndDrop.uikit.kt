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

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.interop.LocalUIKitInteropContainer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.uikit.utils.CMPDragInteractionProxy
import androidx.compose.ui.uikit.utils.CMPDragInteractionProxyMeta
import androidx.compose.ui.viewinterop.interopViewAnchor
import platform.Foundation.NSItemProvider
import platform.UIKit.UIColor
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

private class CupertinoDragAndDropNode(
    view: UIView,
    dragSource: CupertinoDragSource?,
    dropInteractionDelegate: UIDropInteractionDelegateProtocol?,
) : Modifier.Node() {
    var view = view
        set(value) {
            if (field != value) {
                field = value

                updateDragInteraction()
                updateDropInteraction()
            }
        }
    var dragSource = dragSource
        set(value) {
            if (field != value) {
                field = value
                updateDragInteraction()
            }
        }

    private var dragInteractionDelegate: UIDragInteractionDelegateProtocol? = null
    private var dragInteraction: UIDragInteraction? = null

    private var dropInteractionDelegate: UIDropInteractionDelegateProtocol? = null
    private var dropInteraction: UIDropInteraction? = null

    init {
        updateDragInteraction()
        updateDropInteraction()
    }

    private fun updateDragInteraction() {
        // Remove existing interaction if any
        dragInteraction?.let { it.view?.removeInteraction(it) }

        dragInteractionDelegate = dragSource?.toCupertinoDragSourceBridge()

        dragInteractionDelegate?.let { delegate ->
            // Store new interaction and add it to the view
            dragInteraction = UIDragInteraction(delegate = delegate)
                .also { interaction ->
                    view.addInteraction(interaction)
                }
        }
    }

    private fun updateDropInteraction() {
//        // Remove existing interaction if any
//        dropInteraction?.let { it.view?.removeInteraction(it) }
//
//        dropInteractionDelegate?.let { delegate ->
//            // Store new interaction and add it to the view
//            dropInteraction = UIDropInteraction(delegate = delegate)
//                .also { interaction ->
//                    view.addInteraction(interaction)
//                }
//        }
    }
}

private data class CupertinoDragAndDropElement(
    val view: UIView,
    val dragSource: CupertinoDragSource?,
    val dropInteractionDelegate: UIDropInteractionDelegateProtocol?,
) : ModifierNodeElement<CupertinoDragAndDropNode>() {
    override fun create() = CupertinoDragAndDropNode(
        view = view,
        dragSource = dragSource,
        dropInteractionDelegate = dropInteractionDelegate
    )

    override fun update(node: CupertinoDragAndDropNode) {
        node.view = view
        node.dragSource = dragSource
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
    fun itemsForBeginning(session: UIDragSessionProtocol, interaction: UIDragInteraction): List<UIDragItem> = emptyList()

    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2891063-draginteraction
     */
    fun dragSessionIsRestrictedToDraggingApplication(session: UIDragSessionProtocol, interaction: UIDragInteraction): Boolean = false

    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2890978-draginteraction
     */
    fun dragSessionAllowsMoveOperation(session: UIDragSessionProtocol, interaction: UIDragInteraction): Boolean = true
}

@ExperimentalComposeUiApi
interface CupertinoDropTarget {

}

private class CupertinoDragSourceBridge(
    private val dragSource: CupertinoDragSource
) : CMPDragInteractionProxy() {
    override fun itemsForBeginningDragSession(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): List<UIDragItem> = dragSource.itemsForBeginning(session, interaction)

    override fun dragSessionIsRestrictedToDraggingApplication(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): Boolean = dragSource.dragSessionIsRestrictedToDraggingApplication(session, interaction)

    override fun dragSessionAllowsMoveOperation(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): Boolean = dragSource.dragSessionAllowsMoveOperation(session, interaction)
}

private fun CupertinoDragSource.toCupertinoDragSourceBridge() = CupertinoDragSourceBridge(this)

@ExperimentalComposeUiApi
fun String.toNSItemProvider(): NSItemProvider =
    CMPDragInteractionProxy.itemProviderFromString(this)

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
    dragSource: CupertinoDragSource? = null,
    dropInteractionDelegate: UIDropInteractionDelegateProtocol? = null
): Modifier {
    val composedDragAndDropModifier = composed {
        val interopContainer = LocalUIKitInteropContainer.current
        val density = LocalDensity.current
        val view = remember {
            UIView().apply {
                backgroundColor = UIColor.greenColor
            }
        }

        CupertinoDragAndDropElement(
            view = view,
            dragSource = dragSource,
            dropInteractionDelegate = dropInteractionDelegate
        ) then TrackInteropModifierElement(
            container = interopContainer,
            nativeView = view
        ).onGloballyPositioned { coordinates ->
            val rootCoordinates = coordinates.findRootCoordinates()

            val clippedBounds = rootCoordinates
                .localBoundingBoxOf(
                    sourceCoordinates = coordinates,
                    clipBounds = true
                )

            interopContainer.deferAction {
                view.setFrame(clippedBounds.toDpRect(density).asCGRect())
            }
        }.interopViewAnchor(view)
    }

    return composedDragAndDropModifier
}
