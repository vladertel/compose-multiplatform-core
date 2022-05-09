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

package androidx.compose.foundation

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.util.fastAll

class ClickFilterScope internal constructor(
    val event: PointerEvent
) {

    val isPress: Boolean = event.type == PointerEventType.Press
    val isRelease: Boolean = event.type == PointerEventType.Release
    val relatedPointerButton: PointerButton? = if ((isPress || isRelease)) {
        PointerButton(event.changedButton)
    } else {
        null
    }
    val keyModifiers = event.keyboardModifiers

    val isMouse: Boolean = event.changes.fastAll { it.type == PointerType.Mouse }

    fun allChangedToDown() = event.changes.fastAll { it.changedToDown() }

    fun allChangedToUp() = event.changes.fastAll { it.changedToUp() }

    fun allChangesUnconsumed() = event.changes.fastAll { !it.isConsumed }

    override fun toString(): String {
        return event.toString()
    }
}

@JvmInline
value class PointerButton(internal val index: Int) {
    companion object {
        val Primary = PointerButton(1 shl 0)
        val Secondary = PointerButton(1 shl 1)
        val Tertiary = PointerButton(1 shl 2)
        val Back = PointerButton(1 shl 3)
        val Forward = PointerButton(1 shl 4)
    }
}

val PointerButton.isPrimary: Boolean
    get() { return this == PointerButton.Primary }

val PointerButton.isSecondary: Boolean
    get() { return this == PointerButton.Secondary }

val PointerButton.isTertiary: Boolean
    get() { return this == PointerButton.Tertiary }

val PointerButton.isBack: Boolean
    get() { return this == PointerButton.Back }

val PointerButton.isForward: Boolean
    get() { return this == PointerButton.Forward }
