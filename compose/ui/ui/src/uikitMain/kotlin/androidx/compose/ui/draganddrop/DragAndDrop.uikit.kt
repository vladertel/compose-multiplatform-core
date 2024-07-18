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

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.useContents
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
actual class DragAndDropEvent {
    var view: UIView? = null
    var session: UIDropSessionProtocol? = null
}

/**
 * Definition for a type representing transferable data. It could be a remote URI,
 * rich text data on the clip board, a local file, or more.
 */
actual class DragAndDropTransferData(
    val items: List<UIDragItem>
)

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() {
        val view = view ?: return Offset.Unspecified
        val density = view.window?.screen?.nativeScale ?: return Offset.Unspecified
        val session = session ?: return Offset.Unspecified

        val location = session.locationInView(view)

        return location.useContents {
            Offset(x = (x * density).toFloat(), y = (y * density).toFloat())
        }
    }