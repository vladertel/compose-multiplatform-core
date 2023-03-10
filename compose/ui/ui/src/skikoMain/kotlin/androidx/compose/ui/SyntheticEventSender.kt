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
        sendSyntheticMove(event, send)
        sendSyntheticPresses(event, send)
        sendSyntheticReleases(event, send)
        sendInternal(event, send)
    }

    private fun sendSyntheticMove(
        currentEvent: PointerInputEvent,
        send: (PointerInputEvent) -> Unit
    ) {
        val previousEvent = previousEvent
        if (previousEvent != null && isMoveEventMissing(previousEvent, currentEvent)) {
            val idToPosition = currentEvent.pointers.associate { it.id to it.position }
            sendInternal(
                PointerInputEvent(
                    eventType = PointerEventType.Move,
                    pointers = previousEvent.pointers.map {
                        val position = idToPosition[it.id] ?: it.position
                        PointerInputEventData(
                            it.id,
                            it.uptime,
                            position,
                            position,
                            it.down,
                            it.pressure,
                            it.type,
                            it.issuesEnterExit,
                            scrollDelta = Offset(0f, 0f),
                        )
                    },
                    uptime = previousEvent.uptime,
                    nativeEvent = null,
                    buttons = previousEvent.buttons,
                    keyboardModifiers = previousEvent.keyboardModifiers,
                    button = null
                ),
                send
            )
        }
    }

    private fun sendSyntheticPresses(
        currentEvent: PointerInputEvent,
        send: (PointerInputEvent) -> Unit
    ) {
        val previousPressed = previousEvent?.pressedIds().orEmpty()
        val currentPressed = currentEvent.pressedIds()
        val newPressed = (currentPressed - previousPressed).toList()
        println(newPressed)
        val sendingAsDown = HashSet<PointerId>(newPressed.size)
        // Don't send the last pressed pointer (newPressed.size - 1)
        // It will be sent as a real event. Here we only need to send synthetic events before a real one.
        for (i in 0..newPressed.size - 2) {
            val id = newPressed[i]
            sendingAsDown.add(id)
            sendInternal(
                PointerInputEvent(
                    eventType = PointerEventType.Press,
                    pointers = currentEvent.pointers.map {
                        PointerInputEventData(
                            it.id,
                            it.uptime,
                            it.position,
                            it.position,
                            if (it.id in newPressed) sendingAsDown.contains(id) else it.down,
                            it.pressure,
                            it.type,
                            it.issuesEnterExit,
                            scrollDelta = Offset(0f, 0f),
                        )
                    },
                    uptime = currentEvent.uptime,
                    nativeEvent = null,
                    buttons = currentEvent.buttons,
                    keyboardModifiers = currentEvent.keyboardModifiers,
                    button = null
                ),
                send
            )
        }
    }

    private fun sendSyntheticReleases(
        currentEvent: PointerInputEvent,
        send: (PointerInputEvent) -> Unit
    ) {
        val previousPressed = previousEvent?.pressedIds().orEmpty()
        val currentPressed = currentEvent.pressedIds()
        val newReleased = (previousPressed - currentPressed).toList()
        val sendingAsUp = HashSet<PointerId>(newReleased.size)
        // Don't send the first released pointer
        // It will be sent as a real event. Here we only need to send synthetic events before a real one.
        for (i in newReleased.size - 2 downTo 0) {
            val id = newReleased[i]
            sendingAsUp.add(id)
            sendInternal(
                PointerInputEvent(
                    eventType = PointerEventType.Press,
                    pointers = currentEvent.pointers.map {
                        PointerInputEventData(
                            it.id,
                            it.uptime,
                            it.position,
                            it.position,
                            if (it.id in sendingAsUp) !sendingAsUp.contains(id) else it.down,
                            it.pressure,
                            it.type,
                            it.issuesEnterExit,
                            scrollDelta = Offset(0f, 0f),
                        )
                    },
                    uptime = currentEvent.uptime,
                    nativeEvent = null,
                    buttons = currentEvent.buttons,
                    keyboardModifiers = currentEvent.keyboardModifiers,
                    button = null
                ),
                send
            )
        }
    }

    private fun PointerInputEvent.pressedIds(): Set<PointerId> =
        pointers.asSequence().filter { it.down }.mapTo(mutableSetOf()) { it.id }

    private fun sendInternal(event: PointerInputEvent, send: (PointerInputEvent) -> Unit) {
        send(event)
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
}