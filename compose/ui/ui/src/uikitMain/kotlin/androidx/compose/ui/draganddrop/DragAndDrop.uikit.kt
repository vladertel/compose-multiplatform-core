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

import androidx.compose.runtime.ThrowableNSError
import androidx.compose.runtime.toThrowableNSError
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.DropSessionContext
import androidx.compose.ui.uikit.utils.cmp_itemWithObject
import androidx.compose.ui.uikit.utils.cmp_itemWithString
import androidx.compose.ui.uikit.utils.cmp_loadObjectOfClass
import androidx.compose.ui.uikit.utils.cmp_loadString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.useContents
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.Foundation.NSItemProviderWritingProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView
import platform.darwin.NSObject

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
actual class DragAndDropEvent internal constructor(
    private val dropSessionContext: DropSessionContext
) {
    internal val view: UIView
        get() = dropSessionContext.view

    internal val session: UIDropSessionProtocol
        get() = dropSessionContext.session

    /**
     * Drag and drop items associated with this event.
     */
    @ExperimentalComposeUiApi
    val items: List<DragAndDropReceivedItem>
        get() = session.items.map {
            val dragItem = it as UIDragItem

            DragAndDropReceivedItem(
                localObject = dragItem.localObject,
                itemProvider = dragItem.itemProvider
            )
        }
}

@ExperimentalComposeUiApi
class DragAndDropReceivedItem(
    /**
     * Local object associated with the item in case it was set by the application and is consumed
     * within application boundaries.
     */
    val localObject: Any?,

    /**
     * Item provider associated with the item containing a logic for encoding and decoding binary
     * data into a format that can be transferred across process boundaries.
     */
    val itemProvider: NSItemProvider
) {
    /**
     * Attempt to decode an object of type [T] from the [NSItemProvider] if it's contained inside.
     *
     * @param objCClass The ObjC class of the expected object.
     *
     * @return The object of type [T] if it's stored inside the [NSItemProvider], otherwise null.
     *
     * @throws ThrowableNSError if NSItemProvider loading fails.
     * @throws IllegalStateException if the decoded object of class [objCClass] can't be cast to [T].
     */
    @OptIn(BetaInteropApi::class)
    @ExperimentalComposeUiApi
    suspend inline fun <reified T: NSObject> loadObjectOfClass(objCClass: ObjCClass): T? {
        val localObject = localObject
        return if (localObject is T) {
            localObject
        } else {
            // Workaround around the same issue as in `loadString` function.
            val itemProvider = itemProvider

            suspendCoroutine { continuation ->
                itemProvider.cmp_loadObjectOfClass(objCClass) { obj, nsError ->
                    if (nsError != null) {
                        continuation.resumeWithException(nsError.toThrowableNSError())
                    } else {
                        val result = obj as? T

                        if (result != null) {
                            continuation.resume(result)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Failed to cast $obj to expected type"))
                        }
                    }
                }
            }
        }
    }

    /**
     * Attempt to decode a [String] from the [NSItemProvider] if it's contained inside.
     *
     * @return String if it's encoded into the [NSItemProvider], otherwise null.
     *
     * @throws [Throwable] wrapping [NSError] if NSItemProvider's loading fails with an error.
     */
    suspend fun loadString(): String? {
        val localObject = localObject

        return if (localObject is String) {
            localObject
        } else {
            // Workaround for `NSItemProvider.loadObjectOfClass` silently failing
            // and not calling the completion block if the function was dispatched asynchronously
            // from session ending callback.
            val itemProvider = itemProvider

            suspendCoroutine { continuation ->
                itemProvider.cmp_loadString { string, nsError ->
                    if (nsError != null) {
                        continuation.resumeWithException(nsError.toThrowableNSError())
                    } else {
                        continuation.resume(string)
                    }
                }
            }
        }
    }
}

/**
 * On iOS drag and drop session data is represented by [UIDragItem]s, which contains
 * information about how data can be transferred across processes boundaries.
 */
actual class DragAndDropTransferData @ExperimentalComposeUiApi constructor(
    val items: List<DragAndDropTransferDataItem>
) {
    @ExperimentalComposeUiApi
    constructor(scope: DragAndDropTransferDataScope.() -> Unit) : this(
        mutableListOf<DragAndDropTransferDataItem>().apply {
            scope(object : DragAndDropTransferDataScope {
                override fun item(item: DragAndDropTransferDataItem) {
                    add(item)
                }
            })
        }
    )
}

@ExperimentalComposeUiApi
interface DragAndDropTransferDataScope {
    /**
     * Adds a [DragAndDropTransferDataItem] to the drag and drop session.
     */
    fun item(item: DragAndDropTransferDataItem)
}

/**
 * Adds a [String] to the drag and drop session.
 */
fun DragAndDropTransferDataScope.item(string: String): Unit = item(DragAndDropTransferDataItem(string))

/**
 * Creates [DragAndDropTransferDataItem] wrapping the [NSObject] that is assumed to
 * conform to [NSItemProviderWritingProtocol], and adds it to the drag and drop session.
 *
 * If [obj] is not an instance of [objCClass], the application will crash with an assertion
 * failure.
 *
 * @param obj The object to be encoded and transferred.
 * @param objCClass The ObjC class of the [obj]
 *
 * @throws IllegalArgumentException if the [objCClass] doesn't conform to [NSItemProviderWritingProtocol].
 */
@OptIn(BetaInteropApi::class)
fun DragAndDropTransferDataScope.item(obj: NSObject, objCClass: ObjCClass): Unit = item(DragAndDropTransferDataItem(obj, objCClass))


@ExperimentalComposeUiApi
class DragAndDropTransferDataItem(
    internal val uiDragItem: UIDragItem
) {
    constructor(string: String) : this(uiDragItem = UIDragItem.cmp_itemWithString(string))

    /**
     * Creates [DragAndDropTransferDataItem] wrapping the [NSObject] that is assumed to
     * conform to [NSItemProviderWritingProtocol].
     *
     * If [obj] is not an instance of [objCClass], the application will crash with an assertion
     * failure.
     *
     * @param obj The object to be encoded.
     * @param objCClass The ObjC class of the [obj]
     *
     * @throws IllegalArgumentException if the [objCClass] doesn't conform to [NSItemProviderWritingProtocol].
     */
    @OptIn(BetaInteropApi::class)
    constructor(obj: NSObject, objCClass: ObjCClass) : this(
        uiDragItem = requireNotNull(UIDragItem.cmp_itemWithObject(obj, ofClass = objCClass)) {
            "Failed to create UIDragItem from $obj, $objCClass doesn't conform to NSItemProviderWritingProtocol"
        }
    )
}

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() {
        val density = view.window?.screen?.nativeScale ?: return Offset.Unspecified
        val location = session.locationInView(view)

        return location.useContents {
            Offset(x = (x * density).toFloat(), y = (y * density).toFloat())
        }
    }