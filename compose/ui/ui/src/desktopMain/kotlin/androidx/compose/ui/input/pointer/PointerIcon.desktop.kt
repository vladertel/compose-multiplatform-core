/*
 * Copyright 2021 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.Cursor

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalPointerIconService

/**
 * Represents a pointer icon to use in [Modifier.pointerIcon]
 */
@ExperimentalComposeUiApi
interface PointerIcon {
    companion object {
        val Default: PointerIcon = AwtCursor(Cursor(Cursor.DEFAULT_CURSOR))
        val Crosshair: PointerIcon = AwtCursor(Cursor(Cursor.CROSSHAIR_CURSOR))
        val Text: PointerIcon = AwtCursor(Cursor(Cursor.TEXT_CURSOR))
        val Hand: PointerIcon = AwtCursor(Cursor(Cursor.HAND_CURSOR))
    }
}

internal class AwtCursor(val cursor: Cursor) : PointerIcon {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AwtCursor

        if (cursor != other.cursor) return false

        return true
    }

    override fun hashCode(): Int {
        return cursor.hashCode()
    }

    override fun toString(): String {
        return "AwtCursor(cursor=$cursor)"
    }
}

internal interface PointerIconService {
    fun set(icon: PointerIcon)
}

/**
 * Creates [PointerIcon] from [Cursor]
 */
@ExperimentalComposeUiApi
fun PointerIcon(cursor: Cursor): PointerIcon = AwtCursor(cursor)

/**
 * Creates modifier which specifies desired pointer icon when the cursor is over the modified
 * element.
 *
 * @param icon The icon to set
 */
@ExperimentalComposeUiApi
fun Modifier.pointerIcon(icon: PointerIcon) = composed {
    val pointerIconService = LocalPointerIconService.current
    this.pointerMoveFilter(
        onMove = {
            pointerIconService.set(icon)
            false
        }
    )
}