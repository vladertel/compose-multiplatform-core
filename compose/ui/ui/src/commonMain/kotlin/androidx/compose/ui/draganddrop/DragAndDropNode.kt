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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.js.JsName

/**
 * A scope that allows starting a drag and drop session.
 */
interface DragAndDropSourceScope {
    /**
     * Initiates a drag-and-drop operation for transferring data.
     *
     * @param transferData the data to be transferred after successful completion of the drag and
     *   drop gesture.
     * @param decorationSize the size of the drag decoration to be drawn.
     * @param drawDragDecoration provides the visual representation of the item dragged during the
     *   drag and drop gesture.
     * @return true if the method completes successfully, or false if it fails anywhere. Returning
     *   false means the system was unable to do a drag because of another ongoing operation or some
     *   other reasons.
     */
    fun startDragAndDropTransfer(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean
}

/**
 * A [Modifier.Node] providing low level access to platform drag and drop operations. In most cases,
 * you will want to delegate to the [DragAndDropSourceModifierNode] returned by the eponymous factory
 * method.
 */
interface DragAndDropSourceModifierNode : LayoutAwareModifierNode {
    val startTransferRequester: ((Offset) -> Unit)?
}

interface DragAndDropTargetModifierNode : LayoutAwareModifierNode

/**
 * Creates a [Modifier.Node] for starting platform drag and drop sessions with the intention of
 * transferring data.
 */
@OptIn(InternalComposeUiApi::class)
fun DragAndDropSourceModifierNode(
    onStartTransfer: DragAndDropSourceScope.(Offset) -> Unit,
): DragAndDropSourceModifierNode = DragAndDropNode(
    onStartTransfer = onStartTransfer
)

/**
 * Creates a [Modifier.Node] for receiving transfer data from platform drag and drop sessions.
 *
 * @param shouldStartDragAndDrop allows for inspecting the start [DragAndDropEvent] for a given
 *   session to decide whether or not the provided [DragAndDropTarget] would like to receive from
 *   it.
 * @param target allows for receiving events and transfer data from a given drag and drop session.
 */
@JsName("funDragAndDropTargetModifierNode1")
@OptIn(InternalComposeUiApi::class)
fun DragAndDropTargetModifierNode(
    shouldStartDragAndDrop: (event: DragAndDropEvent) -> Boolean,
    target: DragAndDropTarget
): DragAndDropTargetModifierNode = DragAndDropNode(
    onDropTargetValidate = { event ->
        if (shouldStartDragAndDrop(event)) target else null
    }
)

/**
 * Creates a [Modifier.Node] for receiving transfer data from platform drag and drop sessions.
 */
@JsName("funDragAndDropTargetModifierNode2")
@OptIn(InternalComposeUiApi::class)
fun DragAndDropTargetModifierNode(
    onDropTargetValidate: (DragAndDropEvent) -> DragAndDropTarget?
): DragAndDropTargetModifierNode = DragAndDropNode(
    onDropTargetValidate = onDropTargetValidate
)

/**
 * Core implementation of drag and drop. This [Modifier.Node] implements tree traversal for drag and
 * drop, as well as hit testing and propagation of events for drag or drop gestures.
 *
 * It uses the [DragAndDropEvent] as a representation of a single mutable drag and drop session.
 *
 * The implementation implicitly maintains a sorted tree of nodes where the order of traversal is
 * determined by the proximity to the last event. That is, after finding a receiving node, the next
 * event will follow the same path the previous event did unless a fork is found and another node
 * should receive the event.
 *
 * This optimizes traversal for the common case of move events where the event remains within a
 * single node, or moves to a sibling of the node.
 */
@InternalComposeUiApi
class DragAndDropNode(
    private val onStartTransfer: (DragAndDropSourceScope.(Offset) -> Unit)? = null,
    private val onDropTargetValidate: ((DragAndDropEvent) -> DragAndDropTarget?)? = null,
) : Modifier.Node(),
    TraversableNode,
    DragAndDropSourceModifierNode,
    DragAndDropTargetModifierNode,
    DragAndDropTarget {
    companion object {
        private object DragAndDropTraversableKey
    }

    override val traverseKey: Any = DragAndDropTraversableKey

    private val dragAndDropManager: DragAndDropManager
        get() = requireOwner().dragAndDropManager

    /** Child currently receiving drag gestures for dropping into * */
    private var lastChildDragAndDropModifierNode: DragAndDropNode? = null

    /** This as a drop target if eligible for processing * */
    private var thisDragAndDropTarget: DragAndDropTarget? = null

    internal var size: IntSize = IntSize.Zero

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }

    // start Node

    override fun onDetach() {
        // Clean up
        println("onDetach null")
        thisDragAndDropTarget = null
        lastChildDragAndDropModifierNode = null
    }

    // end Node

    // start DragAndDropModifierNode

    override val startTransferRequester: ((Offset) -> Unit)?
        get() = dragAndDropManager
                .startTransferRequester
                .takeIf { onStartTransfer != null }
                ?.let { startTransferRequester ->
                    { offset ->
                        startTransferRequester(this, offset)
                    }
                }

    fun DragAndDropSourceScope.startDragAndDropTransfer(
        offset: Offset,
        isTransferStarted: () -> Boolean
    ) {
        val nodeCoordinates = requireLayoutNode().coordinates
        traverseSelfAndDescendants { currentNode ->
            // TODO: b/303904810 unattached nodes should not be found from an attached
            //  root drag and drop node
            if (!currentNode.isAttached) {
                return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
            }

            if (currentNode.onStartTransfer == null) {
                return@traverseSelfAndDescendants ContinueTraversal
            }

            val currentCoordinates = currentNode.requireLayoutNode().coordinates
            val localPosition = currentCoordinates.localPositionOf(nodeCoordinates, offset)
            if (!currentNode.size.toSize().toRect().contains(localPosition)) {
                return@traverseSelfAndDescendants ContinueTraversal
            }

            currentNode.onStartTransfer.invoke(this, localPosition)

            if (isTransferStarted()) {
                CancelTraversal
            } else {
                ContinueTraversal
            }
        }
    }

    /**
     * The entry point to register interest in a drag and drop session for receiving data.
     *
     * @return true to indicate interest in the contents of a drag and drop session, false indicates
     *   no interest. If false is returned, this [Modifier] will not receive any [DragAndDropTarget]
     *   events.
     */
    fun acceptDragAndDropTransfer(event: DragAndDropEvent): Boolean {
        var handled = false
        traverseSelfAndDescendants { currentNode ->
            // TODO: b/303904810 unattached nodes should not be found from an attached
            //  root drag and drop node
            if (!currentNode.isAttached) {
                return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
            }

            checkPrecondition(currentNode.thisDragAndDropTarget == null) {
                "DragAndDropTarget self reference must be null" +
                    " at the start of a drag and drop session"
            }

            // Start receiving events
            currentNode.thisDragAndDropTarget = currentNode.onDropTargetValidate?.invoke(event)

            val accepted = currentNode.thisDragAndDropTarget != null
            if (accepted) {
                dragAndDropManager.registerNodeInterest(currentNode)
            }
            handled = handled || accepted
            ContinueTraversal
        }
        return handled
    }

    // end DragAndDropModifierNode

    // start DropTarget

    override fun onStarted(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onStarted(event = event)
            else -> self.onStarted(event = event)
        }
    }

    override fun onEntered(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onEntered(event = event)
            else -> self.onEntered(event = event)
        }
    }

    override fun onMoved(event: DragAndDropEvent) {
        val currentChildNode: DragAndDropNode? = lastChildDragAndDropModifierNode
        val newChildNode: DragAndDropNode? =
            when {
                // Moved within child.
                currentChildNode?.contains(event.positionInRoot) == true -> currentChildNode
                // Position is now outside active child, maybe it entered a different one.
                else ->
                    firstDescendantOrNull { child ->
                        // Only dispatch to children who previously accepted the onStart gesture
                        dragAndDropManager.isInterestedNode(child) &&
                            child.contains(event.positionInRoot)
                    }
            }

        when {
            // Left us and went to a child.
            newChildNode != null && currentChildNode == null -> {
                newChildNode.dispatchEntered(event)
                thisDragAndDropTarget?.onExited(event = event)
            }
            // Left the child and returned to us.
            newChildNode == null && currentChildNode != null -> {
                thisDragAndDropTarget?.dispatchEntered(event)
                currentChildNode.onExited(event = event)
            }
            // Left one child and entered another.
            newChildNode != currentChildNode -> {
                newChildNode?.dispatchEntered(event)
                currentChildNode?.onExited(event = event)
            }
            // Stayed in the same child.
            newChildNode != null -> newChildNode.onMoved(event = event)
            // Stayed in us.
            else -> thisDragAndDropTarget?.onMoved(event = event)
        }

        this@DragAndDropNode.lastChildDragAndDropModifierNode = newChildNode
    }

    override fun onChanged(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onChanged(event = event)
            else -> self.onChanged(event = event)
        }
    }

    override fun onExited(event: DragAndDropEvent) {
        thisDragAndDropTarget?.onExited(event = event)
        lastChildDragAndDropModifierNode?.onExited(event = event)
        lastChildDragAndDropModifierNode = null
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        return when (val currentChildDropTarget = lastChildDragAndDropModifierNode) {
            null -> thisDragAndDropTarget?.onDrop(event = event) ?: false
            else -> currentChildDropTarget.onDrop(event = event)
        }
    }

    override fun onEnded(event: DragAndDropEvent) = traverseSelfAndDescendants { currentNode ->
        // TODO: b/303904810 unattached nodes should not be found from an attached
        //  root drag and drop node
        if (!currentNode.node.isAttached) {
            return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
        }
        currentNode.thisDragAndDropTarget?.onEnded(event = event)
        currentNode.thisDragAndDropTarget = null
        currentNode.lastChildDragAndDropModifierNode = null
        ContinueTraversal
    }
    // end DropTarget
}

private fun DragAndDropTarget.dispatchEntered(event: DragAndDropEvent) = run {
    // Notify of entry
    onEntered(event = event)
    // Start move
    onMoved(event = event)
}

/** Hit test for a [DragAndDropNode]. */
@OptIn(InternalComposeUiApi::class)
private fun DragAndDropNode.contains(positionInRoot: Offset): Boolean {
    if (!node.isAttached) return false
    val currentCoordinates = requireLayoutNode().coordinates
    if (!currentCoordinates.isAttached) return false

    val (x1, y1) = currentCoordinates.positionInRoot()

    // Use measured size instead of size from currentCoordinates because it might be different
    //  (eg if padding is applied)
    val x2 = x1 + size.width
    val y2 = y1 + size.height

    return positionInRoot.x in x1..x2 && positionInRoot.y in y1..y2
}

private fun <T : TraversableNode> T.traverseSelfAndDescendants(
    block: (T) -> TraverseDescendantsAction
) {
    if (block(this) != ContinueTraversal) return
    traverseDescendants(block)
}

private inline fun <T : TraversableNode> T.firstDescendantOrNull(
    crossinline predicate: (T) -> Boolean
): T? {
    // TODO: b/303904810 unattached nodes should not be found from an attached
    //  root drag and drop node
    if (!node.isAttached) return null
    var match: T? = null
    traverseDescendants { child ->
        if (predicate(child)) {
            match = child
            return@traverseDescendants CancelTraversal
        }
        ContinueTraversal
    }
    return match
}
