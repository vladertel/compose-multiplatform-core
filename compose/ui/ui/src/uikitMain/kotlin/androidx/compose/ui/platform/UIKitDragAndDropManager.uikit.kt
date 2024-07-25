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

package androidx.compose.ui.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropNode
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.draganddrop.DragAndDropSourceScope
import androidx.compose.ui.draganddrop.cupertino.loadString
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.uikit.utils.CMPDragInteractionProxy
import androidx.compose.ui.uikit.utils.CMPDropInteractionProxy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.window.InteractionUIView
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.UIKit.UIColor
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropOperationForbidden
import platform.UIKit.UIDropProposal
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.addInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDropOperation
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIPreviewParameters
import platform.UIKit.UIPreviewTarget
import platform.UIKit.UITargetedDragPreview
import platform.UIKit.UIView


private class DragAndDropSessionContext(
    val transferData: DragAndDropTransferData,
    val decorationSize: Size,
    val drawDragDecoration: DrawScope.() -> Unit
) {
    private var preview: UITargetedDragPreview? = null

    fun getPreview(view: UIView, session: UIDragSessionProtocol): UITargetedDragPreview {
        return preview ?: generatePreview(view, session).also {
            preview = it
        }
    }

    fun generatePreview(view: UIView, session: UIDragSessionProtocol): UITargetedDragPreview {
        val window = checkNotNull(view.window) {
            "Can't generate UITargetedDragPreview for a view, detached from the window"
        }

        val density = Density(density = window.screen.scale.toFloat())

        val decorationCgRect = decorationSize
            .toRect()
            .toDpRect(density)
            .asCGRect()

        val decorationView = UIView(frame = decorationCgRect)
        decorationView.backgroundColor = UIColor.blueColor

        val preview = UITargetedDragPreview(
            view = decorationView,
            parameters = UIPreviewParameters(),
            target = UIPreviewTarget(
                container = view,
                center = session.locationInView(view),
                transform = CGAffineTransformIdentity.readValue()
            )
        )
        return preview
    }
}

/**
 * The [DragAndDropManager] implementation
 * for UIKit.
 *
 * This class is responsible for managing the drag and drop interactions on the UIKit platform.
 * It is responsible for setting up the drag and drop interactions on the [view] and bridging the
 * context between iOS and Compose.
 */
internal class UIKitDragAndDropManager(
    val view: InteractionUIView,
) : DragAndDropManager {
    /**
     * Context for an ongoing drag and drop session.
     */
    private var sessionContext: DragAndDropSessionContext? = null

    /**
     * The [CMPDragInteractionProxy] that handles the serves as [UIDragInteractionDelegateProtocol] for the
     * interaction added to [view]
     */
    private val dragInteractionProxy = object : CMPDragInteractionProxy() {
        override fun isSessionRestrictedToDraggingApplication(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): Boolean {
            return false
        }

        override fun itemsForBeginningSession(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): List<*> {
            val scope = object : DragAndDropSourceScope {
                override fun startDragAndDropTransfer(
                    transferData: DragAndDropTransferData,
                    decorationSize: Size,
                    drawDragDecoration: DrawScope.() -> Unit
                ): Boolean {
                    sessionContext = DragAndDropSessionContext(
                        transferData = transferData,
                        decorationSize = decorationSize,
                        drawDragDecoration = drawDragDecoration
                    )
                    return true
                }
            }

            val location = session.locationInView(view)

            with(rootDragAndDropNode) {
                scope.onStartTransfer(Offset.Zero) // TODO
            }

            return sessionContext?.transferData?.items ?: emptyList<UIDragItem>()
        }

        override fun previewForLiftingItemInSession(
            session: UIDragSessionProtocol,
            item: UIDragItem,
            interaction: UIDragInteraction
        ): UITargetedDragPreview? = withSessionContext {
            getPreview(view, session)
        }

        override fun doesSessionAllowMoveOperation(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): Boolean {
            return true
        }

        override fun sessionDidEndWithOperation(
            session: UIDragSessionProtocol,
            interaction: UIDragInteraction,
            operation: UIDropOperation
        ) {
            sessionContext = null
            interestedNodes.clear()
        }
    }

    private val dropInteractionProxy = object : CMPDropInteractionProxy() {
        override fun canHandleSession(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ): Boolean {
            return false
        }

        override fun performDropFromSession(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                for (item in session.items) {
                    if (item !is UIDragItem) return@launch
                    val text = item.loadString()
                    println("Dropped string: $text")
                }
            }
        }

        override fun proposalForSessionUpdate(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ): UIDropProposal {
            return UIDropProposal(UIDropOperationForbidden)
        }

        override fun sessionDidEnd(session: UIDropSessionProtocol, interaction: UIDropInteraction) {
            sessionContext = null
            interestedNodes.clear()
        }
    }

    /**
     * The root [DragAndDropNode] that is used to perform traversal of the drag and drop aware
     * nodes in the hierarchy. `null` returned implies that the root node is not an actual
     * [DragAndDropTarget].
     */
    private val rootDragAndDropNode = DragAndDropNode()

    /**
     * The [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     */
    override val modifier: Modifier = DragAndDropModifier(rootDragAndDropNode)

    private val interestedNodes = mutableSetOf<DragAndDropModifierNode>()

    init {
        view.addInteraction(UIDragInteraction(delegate = dragInteractionProxy))
        view.addInteraction(UIDropInteraction(delegate = dropInteractionProxy))
    }

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }

    private fun <R> withSessionContext(block: DragAndDropSessionContext.() -> R): R? =
        sessionContext?.block()
}