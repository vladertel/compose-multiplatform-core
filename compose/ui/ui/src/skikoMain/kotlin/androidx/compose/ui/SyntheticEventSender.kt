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

package androidx.compose.ui

import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import org.jetbrains.skia.Image

interface GG {
    fun gg(): Any
}

class HH: GG {
    override fun gg(): Image {
        TODO()
    }
}

/**
 * Compose or user code can't work well if we miss some events.
 *
 * For example:
 * - if we miss Move before Press event with a different position
 * - if we send one event with 2 pressed touches without sending 1 pressed touch first
 *
 * Platforms can send events this way.
 *
 * This class generates new synthetic events based on the previous event, if something is missing
 */
internal class SyntheticEventSender {
    private var previousEvent: PointerInputEvent? = null

    /**
     * Send [event] and synthetic events before it if needed. On each sent event we just call [send]
     */
    fun send(
        event: PointerInputEvent,
        send: (PointerInputEvent) -> Unit
    ) {
        previousEvent?.let { sendSynthetic(it, event, send) }
        send(event)
        // We don't send nativeEvent for synthetic events.
        // Nullify to avoid memory leaks (native events can point to native views).
        previousEvent = event.copy(nativeEvent = null)
    }

    private fun sendSynthetic(
        previousEvent: PointerInputEvent,
        currentEvent: PointerInputEvent,
        send: (PointerInputEvent) -> Unit
    ) {
        if (isMoveEventMissing(previousEvent, currentEvent)) {
            send(createEvent(PointerEventType.Move, previousEvent, currentEvent))
        }
    }

    private fun isMoveEventMissing(
        previousEvent: PointerInputEvent?,
        currentEvent: PointerInputEvent,
    ) = !currentEvent.isMove() && !currentEvent.isSamePosition(previousEvent)

    private fun PointerInputEvent.isMove() =
        eventType == PointerEventType.Move ||
            eventType == PointerEventType.Enter ||
            eventType == PointerEventType.Exit

    private fun PointerInputEvent.isSamePosition(previousEvent: PointerInputEvent?): Boolean {
        val previousIdToPosition = previousEvent?.pointers?.associate { it.id to it.position }
        return pointers.all {
            val previousPosition = previousIdToPosition?.get(it.id)
            previousPosition == null || it.position == previousPosition
        }
    }
}