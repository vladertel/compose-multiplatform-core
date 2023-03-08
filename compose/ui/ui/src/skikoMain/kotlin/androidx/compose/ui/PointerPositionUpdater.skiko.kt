/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.input.pointer.HitPathTracker
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import kotlin.js.JsName

internal class SyntheticEventSender(
    private val onNeedSend: () -> Unit,
    private val sendEvent: (
        eventType: PointerEventType,
        sourceEvent: PointerInputEvent,
        positionSourceEvent: PointerInputEvent
    ) -> Unit,
) {
    private var lastEvent: PointerInputEvent? = null

    var needSendMove: Boolean = false
        private set

    fun reset() {
        lastEvent = null
        needSendMove = false
    }

    fun beforeEvent(event: PointerInputEvent) {
        if (isMoveEventMissing(lastEvent, event) || needSendMove) {
            needSendMove = false
            if (!event.isMove()) {
                lastEvent?.sendWith(positionSourceEvent = event)
            }
        }
        lastEvent = event
    }

    @JsName("setNeedUpdate")
    fun needSendMove() {
        needSendMove = true
        onNeedSend()
    }

    fun beforeDraw() {
        if (needSendMove) {
            needSendMove = false
            lastEvent?.also { it.sendWith(positionSourceEvent = it) }
        }
    }

    private fun PointerInputEvent.sendWith(positionSourceEvent: PointerInputEvent) {
        if (positionSourceEvent.pointers.isNotEmpty()) {
            sendEvent(PointerEventType.Move, this, positionSourceEvent)
        }
    }
}

/**
 * Compose can't work well if we miss Move event before, for example, a Scroll event.
 *
 * This is because of the implementation of [HitPathTracker].
 *
 * Imaging two boxes:
 * ```
 * Column {
 *   Box(size=10)
 *   Box(size=10)
 * }
 * ```
 *
 * - we send Move's in the right order:
 * 1. Move(5,5) -> box1 receives Enter(5,5)
 * 2. Move(5,15) -> box1 receives Exit(5,15), box2 receives Enter(5,15)
 * 3. Scroll(5,15) -> box2 receives Scroll(5,15)
 *
 * - we skip some Move's between last move and current Scroll (AWT, for example, can skip them):
 * 1. Move(5,5) -> box1 receives Enter(5,5)
 * 2. Scroll(5,15) -> box1 receives Scroll(5,15), box2 receives Scroll(5,15)
 * 3. Move(5,16) -> box2 receives Enter(5,16)
 *
 * You can see that box1 loses the Exit event (instead it receives Scroll event)
 */
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