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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData

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
internal class SyntheticEventSender(
    send: (PointerInputEvent) -> Unit
) {
    private val _send: (PointerInputEvent) -> Unit = send
    private var previousEvent: PointerInputEvent? = null

    fun reset() {
        previousEvent = null
    }

    /**
     * Send [event] and synthetic events before it if needed. On each sent event we just call [send]
     */
    fun send(event: PointerInputEvent) {
        sendSyntheticMove(event)
        sendSyntheticReleases(event)
        sendSyntheticPresses(event)
        sendInternal(event)
    }

    fun sendPreviousMove() {
        val previousEvent = previousEvent ?: return
        val idToPosition = previousEvent.pointers.associate { it.id to it.position }
        sendInternal(
            previousEvent.copySynthetic(PointerEventType.Move) {
                copySynthetic(position = idToPosition[id] ?: position)
            }
        )
    }

    private fun sendSyntheticMove(currentEvent: PointerInputEvent) {
        val previousEvent = previousEvent ?: return
        if (isMoveEventMissing(previousEvent, currentEvent)) {
            val idToPosition = currentEvent.pointers.associate { it.id to it.position }
            sendInternal(
                previousEvent.copySynthetic(PointerEventType.Move) {
                    copySynthetic(position = idToPosition[id] ?: position)
                }
            )
        }
    }

    private fun sendSyntheticReleases(currentEvent: PointerInputEvent) {
        val previousEvent = previousEvent ?: return
        val previousPressed = previousEvent.pressedIds()
        val currentPressed = currentEvent.pressedIds()
        val newReleased = (previousPressed - currentPressed).toList()
        val sendingAsUp = HashSet<PointerId>(newReleased.size)

        // Don't send the first released pointer
        // It will be sent as a real event. Here we only need to send synthetic events before a real one.
        for (i in newReleased.size - 2 downTo 0) {
            sendingAsUp.add(newReleased[i])

            sendInternal(
                previousEvent.copySynthetic(PointerEventType.Release) {
                    copySynthetic(
                        down = if (id in sendingAsUp) !sendingAsUp.contains(id) else down
                    )
                }
            )
        }
    }

    private fun sendSyntheticPresses(currentEvent: PointerInputEvent) {
        println("Q3")
        val previousPressed = previousEvent?.pressedIds().orEmpty()
        val currentPressed = currentEvent.pressedIds()
        val newPressed = (currentPressed - previousPressed).toList()
        val sendingAsDown = HashSet<PointerId>(newPressed.size)

        println("Q4 $newPressed")
        // Don't send the last pressed pointer (newPressed.size - 1)
        // It will be sent as a real event. Here we only need to send synthetic events before a real one.
        for (i in 0..newPressed.size - 2) {
            sendingAsDown.add(newPressed[i])

            println("Q5 ${currentEvent.pointers.map { it.id }}")
            sendInternal(
                currentEvent.copySynthetic(PointerEventType.Press) {
                    copySynthetic(
                        down = if (id in newPressed) sendingAsDown.contains(id) else down
                    )
                }
            )
        }
    }

    private fun PointerInputEvent.pressedIds(): Set<PointerId> =
        pointers.asSequence().filter { it.down }.mapTo(mutableSetOf()) { it.id }

    private fun sendInternal(event: PointerInputEvent) {
        _send(event)
        // We don't send nativeEvent for synthetic events.
        // Nullify to avoid memory leaks (native events can point to native views).
        previousEvent = event.copy(nativeEvent = null)
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

    // we don't use copy here to not forget to nullify properties that shouldn't be in a synthetic event
    private fun PointerInputEvent.copySynthetic(
        type: PointerEventType,
        pointer: PointerInputEventData.() -> PointerInputEventData,
    ) = PointerInputEvent(
        eventType = type,
        pointers = pointers.map(pointer),
        uptime = uptime,
        nativeEvent = null,
        buttons = buttons,
        keyboardModifiers = keyboardModifiers,
        button = null
    )

    private fun PointerInputEventData.copySynthetic(
        position: Offset = this.position,
        down: Boolean = this.down
    ) = PointerInputEventData(
        id,
        uptime,
        position,
        position,
        down,
        pressure,
        type,
        issuesEnterExit,
        scrollDelta = Offset(0f, 0f),
    )
}