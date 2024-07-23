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
import androidx.compose.ui.draganddrop.DragAndDropSessionGatingGestureRecognizer
import androidx.compose.ui.draganddrop.DragAndDropSessionGatingInterruptionOutcome
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.cupertino.loadString
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.uikit.utils.CMPDragInteractionProxy
import androidx.compose.ui.uikit.utils.CMPDropInteractionProxy
import androidx.compose.ui.window.InteractionUIView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropOperationForbidden
import platform.UIKit.UIDropProposal
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.addInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIGestureRecognizer

internal class UIKitDragAndDropManager(
    val view: InteractionUIView,
) : PlatformDragAndDropManager {
    var currentTransferData: DragAndDropTransferData? = null

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
            return listOf<Any>()
        }

        override fun doesSessionAllowMoveOperation(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): Boolean {
            return true
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

        //sessio
    }

    /**
     * Gesture recognizer that gates the drag and drop session start until we explicitly allow it
     * by interrupting its recognition.
     *
     * @see DragAndDropSessionGatingGestureRecognizer
     * @see DragAndDropSessionGatingInterruptionOutcome
     */
    private val dragAndDropSessionGatingGestureRecognizer =
        DragAndDropSessionGatingGestureRecognizer()

    /**
     * The root [DragAndDropNode] that is used to perform traversal of the drag and drop aware
     * nodes in the hierarchy. `null` returned implies that the root node is not an actual
     * [DragAndDropTarget].
     */
    private val rootDragAndDropNode = DragAndDropNode { null }

    /**
     * The [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     */
    override val modifier: Modifier = DragAndDropModifier(rootDragAndDropNode)

    private val interestedNodes = mutableSetOf<DragAndDropModifierNode>()

    init {
        val getViewGestureRecognizersSet = {
            val list = view.gestureRecognizers ?: emptyList<Any>()
            list.map { it as UIGestureRecognizer }.toSet()
        }

        val preInteractionAddedSet = getViewGestureRecognizersSet()

        view.addInteraction(UIDragInteraction(delegate = dragInteractionProxy))
        view.addInteraction(UIDropInteraction(delegate = dropInteractionProxy))

        val postInteractionAddedSet = getViewGestureRecognizersSet()
        val addedGestureRecognizers = postInteractionAddedSet - preInteractionAddedSet

        dragAndDropSessionGatingGestureRecognizer.configure(view, addedGestureRecognizers.toList())
    }

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        val interruptionOutcome = dragAndDropSessionGatingGestureRecognizer.interrupt()

        return when (interruptionOutcome) {
            DragAndDropSessionGatingInterruptionOutcome.POTENTIAL_SUCCESS -> {
                // The drag and drop session can start and is not gated by this gesture recognizer.
                return true
            }

            DragAndDropSessionGatingInterruptionOutcome.FAILURE -> {
                // The drag and drop session is not possible
                return false
            }
        }
    }

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }
}