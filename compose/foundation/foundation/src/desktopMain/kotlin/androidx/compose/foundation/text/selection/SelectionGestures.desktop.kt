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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.contextMenuOpenDetector
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange


private inline fun Modifier.onMacOsOnly(crossinline modifier: Modifier.() -> Modifier) = this.then(
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        this.modifier()
    } else {
        Modifier
    }
)

/**
 * Returns whether the given pixel position is inside the selection.
 */
internal fun TextLayoutResult.isPositionInsideSelection(
    selectionRange: TextRange?,
    position: Offset
): Boolean {
    if ((selectionRange == null) || selectionRange.collapsed) return false

    fun isOffsetSelectedAndContainsPosition(offset: Int) =
        selectionRange.contains(offset) && getBoundingBox(offset).contains(position)

    // getOffsetForPosition returns the index at which the cursor should be placed when the given
    // position is clicked. This means that when position is to the right of the center of a glyph
    // it will return the index of the next glyph. So we test both the index it returns and the
    // previous index.
    val offset = getOffsetForPosition(position)
    return isOffsetSelectedAndContainsPosition(offset) ||
        isOffsetSelectedAndContainsPosition(offset-1)
}

/**
 * A version of [contextMenuOpenDetector] that doesn't consume the event.
 *
 * The event isn't consumed when selecting the word on right-click because that event needs to also
 * open the context menu, which is handled elsewhere.
 */
private inline fun Modifier.nonConsumingContextMenuOpenDetector(
    pass: PointerEventPass,
    crossinline onOpen: (Offset) -> Unit
) = contextMenuOpenDetector(pass = pass) { position ->
    onOpen(position)
    false
}

internal fun Modifier.macOsSelectWordOnRightClick(
    selectionRegistrar: SelectionRegistrar,
    selectableId: Long,
    layoutCoordinates: () -> LayoutCoordinates?,
    textLayoutResult: () -> TextLayoutResult?
) = onMacOsOnly {
    nonConsumingContextMenuOpenDetector(
        pass = PointerEventPass.Initial,
    ) { clickPosition ->
        val layoutCoords = layoutCoordinates()
        if ((layoutCoords == null) || !layoutCoords.isAttached)
            return@nonConsumingContextMenuOpenDetector

        val textLayout = textLayoutResult() ?: return@nonConsumingContextMenuOpenDetector
        val isClickedPositionInsideSelection = textLayout.isPositionInsideSelection(
            selectionRange = selectionRegistrar.subselections[selectableId]?.toTextRange(),
            position = clickPosition
        )
        if (!isClickedPositionInsideSelection) {
            selectionRegistrar.notifySelectionUpdateStart(
                layoutCoordinates = layoutCoords,
                startPosition = clickPosition,
                adjustment = SelectionAdjustment.Word,
                isInTouchMode = false
            )
            selectionRegistrar.notifySelectionUpdateEnd()
        }
    }
}

internal fun Modifier.macOsSelectWordOnRightClick(
    manager: TextFieldSelectionManager
) = onMacOsOnly {
    nonConsumingContextMenuOpenDetector(pass = PointerEventPass.Initial) { clickPosition ->
        val layoutResult = manager.state?.layoutResult ?: return@nonConsumingContextMenuOpenDetector
        val textLayout = layoutResult.value
        val isClickedPositionInsideSelection = textLayout.isPositionInsideSelection(
            selectionRange = manager.value.selection,
            position = layoutResult.translateDecorationToInnerCoordinates(clickPosition)
        )
        if (!isClickedPositionInsideSelection) {
            // Adapted from AwaitPointerEventScope.mouseSelection on double-click
            val observer = manager.mouseSelectionObserver
            val started = observer.onStart(clickPosition, SelectionAdjustment.Word)
            if (started) {
                observer.onDragDone()
            }
        }
    }
}