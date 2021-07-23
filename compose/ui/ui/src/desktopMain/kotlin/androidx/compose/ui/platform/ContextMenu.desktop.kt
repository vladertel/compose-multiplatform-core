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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect

@ExperimentalComposeUiApi
class ContextMenuItem(
    val label: String,
    val onClick: () -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContextMenuItem

        if (label != other.label) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContextMenuItem(label='$label')"
    }
}

@ExperimentalComposeUiApi
class ContextMenuData(
    val items: () -> List<ContextMenuItem>,
    val next: ContextMenuData?
) {

    val itemsSeq: Sequence<ContextMenuItem>
        get() =
            sequence {
                yieldAll(items())
                next?.let { yieldAll(it.itemsSeq) }
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContextMenuData

        if (items != other.items) return false
        if (next != other.next) return false

        return true
    }

    override fun hashCode(): Int {
        var result = items.hashCode()
        result = 31 * result + (next?.hashCode() ?: 0)
        return result
    }
}

@ExperimentalComposeUiApi
class ContextMenuState {
    sealed class Status {
        class Open(
            val rect: Rect
        ) : Status() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Open

                if (rect != other.rect) return false

                return true
            }

            override fun hashCode(): Int {
                return rect.hashCode()
            }

            override fun toString(): String {
                return "Open(rect=$rect)"
            }
        }

        object Closed : Status()
    }

    var status: Status by mutableStateOf(Status.Closed)
}

@ExperimentalComposeUiApi
interface ContextMenuRepresentation {
    @Composable
    fun Representation(state: ContextMenuState, data: ContextMenuData)
}

@ExperimentalComposeUiApi
val LocalContextMenuRepresentation =
    staticCompositionLocalOf<ContextMenuRepresentation?> {
        null
    }