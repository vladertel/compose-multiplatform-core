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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package androidx.compose.desktop

import androidx.compose.foundation.ContextMenuData
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.Ref

internal val contextMenuRepresentation = object : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, data: ContextMenuData) {
        val isOpen = state.status is ContextMenuState.Status.Open
        val previousItems = remember { Ref<List<ContextMenuItem>>() }
        val items = if (isOpen) {
            remember { data.itemsSeq.toList() }
        } else {
            previousItems.value ?: emptyList()
        }
        previousItems.value = items

        CursorDropdownMenu(
            expanded = state.status is ContextMenuState.Status.Open,
            onDismissRequest = { state.status = ContextMenuState.Status.Closed }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        state.status = ContextMenuState.Status.Closed
                        item.onClick()
                    }
                ) {
                    Text(text = item.label)
                }
            }
        }
    }
}