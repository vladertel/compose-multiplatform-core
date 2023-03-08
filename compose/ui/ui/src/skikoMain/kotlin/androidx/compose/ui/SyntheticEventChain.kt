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

/**
 * Compose or user code can't work well if we miss some events.
 *
 * For example:
 * - if we miss Move before Press event with a different position
 * - if we send one event with 2 pressed touches without sending 1 pressed touch first
 *
 * Platforms can send events this way.
 *
 * This function generate new synthetic events based on the previous event, if something is missing
 */
internal fun syntheticEventSequence(
    vararg inputSequence: PointerInputEvent
): List<PointerInputEvent> = syntheticEventSequence(
    inputSequence.asSequence()
).toList()

internal fun syntheticEventSequence(
    inputSequence: Sequence<PointerInputEvent>
): Sequence<PointerInputEvent> = sequence {
    var previousEvent: PointerInputEvent? = null
    for (currentEvent in inputSequence) {
        previousEvent?.let { sendSyntheticEvents(it, currentEvent) }
        yield(currentEvent)
        previousEvent = currentEvent
    }
}

private suspend fun SequenceScope<PointerInputEvent>.sendSyntheticEvents(
    previousEvent: PointerInputEvent,
    currentEvent: PointerInputEvent
) {
    if (isMoveEventMissing(previousEvent, currentEvent)) {
        yield(createEvent(PointerEventType.Move, previousEvent, currentEvent))
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