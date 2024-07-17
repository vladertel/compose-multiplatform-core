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

package androidx.compose.ui.draganddrop.cupertino

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
import androidx.compose.ui.uikit.utils.CMPDropInteractionProxy
import androidx.compose.ui.uikit.utils.itemWithString
import androidx.compose.ui.viewinterop.interopViewAnchor
import platform.UIKit.UIColor
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropInteractionDelegateProtocol
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView
import platform.UIKit.addInteraction
import platform.UIKit.removeInteraction
import platform.UIKit.UIEvent
import platform.UIKit.UIImageView

private class DragAndDropNode(
    view: UIView,
    dragSource: DragSource?,
    dropTarget: DropTarget?,
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

    var dropTarget = dropTarget
        set(value) {
            if (field != value) {
                field = value
                updateDropInteraction()
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

        dragInteractionDelegate = dragSource?.toInteractionDelegate()

        dragInteractionDelegate?.let { delegate ->
            // Store new interaction and add it to the view
            dragInteraction = UIDragInteraction(delegate = delegate)
                .also { interaction ->
                    view.addInteraction(interaction)
                }
        }
    }

    private fun updateDropInteraction() {
        // Remove existing interaction if any
        dropInteraction?.let { it.view?.removeInteraction(it) }

        dropInteractionDelegate = dropTarget?.toInteractionDelegate()

        dropInteractionDelegate?.let { delegate ->
            // Store new interaction and add it to the view
            dropInteraction = UIDropInteraction(delegate = delegate)
                .also { interaction ->
                    view.addInteraction(interaction)
                }
        }
    }
}

private data class DragAndDropElement(
    val view: UIView,
    val dragSource: DragSource?,
    val dropTarget: DropTarget?,
) : ModifierNodeElement<DragAndDropNode>() {
    override fun create() = DragAndDropNode(
        view = view,
        dragSource = dragSource,
        dropTarget = dropTarget
    )

    override fun update(node: DragAndDropNode) {
        node.view = view
        node.dragSource = dragSource
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cupertinoDragSource"
    }
}


@ExperimentalComposeUiApi
interface DragSource {
    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2891010-draginteraction
     */
    fun UIDragInteraction.itemsForBeginningSession(session: UIDragSessionProtocol): List<UIDragItem> = emptyList()

    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2891063-draginteraction
     */
    fun UIDragInteraction.isSessionRestrictedToDraggingApplication(session: UIDragSessionProtocol): Boolean = false

    /**
     * https://developer.apple.com/documentation/uikit/uidraginteractiondelegate/2890978-draginteraction
     */
    fun UIDragInteraction.doesSessionAllowMoveOperation(session: UIDragSessionProtocol): Boolean = true
}

@ExperimentalComposeUiApi
fun String.toUIDragItem(): UIDragItem = UIDragItem.itemWithString(this)

private class DragSourceToInteractionDelegateAdapter(
    private val dragSource: DragSource,
) : CMPDragInteractionProxy() {


    override fun itemsForBeginningSession(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): List<UIDragItem> = delegating {
        with(interaction) {
            itemsForBeginningSession(session)
        }
    }

    override fun isSessionRestrictedToDraggingApplication(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): Boolean = delegating {
        with(interaction) {
            isSessionRestrictedToDraggingApplication(session)
        }
    }

    override fun doesSessionAllowMoveOperation(
        session: UIDragSessionProtocol,
        interaction: UIDragInteraction
    ): Boolean = delegating {
        with(interaction) {
            doesSessionAllowMoveOperation(session)
        }
    }

    private fun <R> delegating(block: DragSource.() -> R): R = dragSource.run(block)
}

private fun DragSource.toInteractionDelegate(): UIDragInteractionDelegateProtocol =
    DragSourceToInteractionDelegateAdapter(this)


@ExperimentalComposeUiApi
interface DropTarget {
    /**
     * https://developer.apple.com/documentation/uikit/uidropinteractiondelegate/2891010-dropinteraction
     */
    fun UIDropInteraction.canHandleSession(session: UIDropSessionProtocol): Boolean = false

    /**
     * https://developer.apple.com/documentation/uikit/uidropinteractiondelegate/2890889-dropinteraction
     */
    fun UIDropInteraction.performDropFromSession(session: UIDropSessionProtocol) = Unit
}

/**
 * A bridging class that adapts [DropTarget] to [UIDropInteractionDelegateProtocol].
 */
private class DropTargetToInteractionDelegateAdapter(
    private val dropTarget: DropTarget
) : CMPDropInteractionProxy() {

        override fun canHandleSession(
            session: UIDropSessionProtocol,
            interaction: UIDropInteraction
        ): Boolean = delegating {
            with(interaction) {
                canHandleSession(session)
            }
        }

        override fun performDropFromSession(
            session: UIDropSessionProtocol,
            interaction: UIDropInteraction
        ) = delegating {
            with(interaction) {
                performDropFromSession(session)
            }
        }

        private fun <R> delegating(block: DropTarget.() -> R): R = dropTarget.run(block)
}

private fun DropTarget.toInteractionDelegate(): UIDropInteractionDelegateProtocol =
    DropTargetToInteractionDelegateAdapter(this)

/**
 * Modifier that adds an invisible view to the hierarchy that can be used as a drag-and-drop source
 * or target.
 *
 * On iOS  drag-and-drop sessions can only be triggered by internal API of native views via adding
 * [UIDragInteraction] and [UIDropInteraction]. It's not allowed to start a drag-and-drop session
 * imperatively.
 *
 * This modifier allows to add a drag-and-drop session to a view and handle the drop event.
 * Compose merely controls the position and size of the dummy views and forwards events of the
 * drag-n-drop session via [dragSource] and [dropTarget].
 */
@ExperimentalComposeUiApi
fun Modifier.dragAndDrop(
    dragSource: DragSource? = null,
    dropTarget: DropTarget? = null
): Modifier {
    val composedDragAndDropModifier = composed {
        val interopContainer = LocalUIKitInteropContainer.current
        val density = LocalDensity.current
        val view = remember {
            UIImageView().apply {
                setUserInteractionEnabled(true)
                backgroundColor = UIColor.greenColor
            }
        }

        DragAndDropElement(
            view = view,
            dragSource = dragSource,
            dropTarget = dropTarget,
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
