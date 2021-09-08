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

package androidx.compose.foundation

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.collect

internal actual val BasicContextMenuRepresentation = object : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, data: ContextMenuData) {
        val view = LocalView.current
        var actionMode: ActionMode? by remember { mutableStateOf(null) }
        val contextMenuCallback = remember { ContextMenuCallback(state) }
        LaunchedEffect(state, data) {
            snapshotFlow { state.status }.collect { currentStatus ->
                if (currentStatus is ContextMenuState.Status.Open) {
                    contextMenuCallback.data = data
                    if (actionMode == null) {
                        actionMode = if (Build.VERSION.SDK_INT >= 23) {
                            TextToolbarHelperMethods.startActionMode(
                                view,
                                FloatingTextActionModeCallback(
                                    currentStatus.rect,
                                    contextMenuCallback
                                ),
                                ActionMode.TYPE_FLOATING
                            )
                        } else {
                            view.startActionMode(
                                PrimaryTextActionModeCallback(contextMenuCallback)
                            )
                        }
                    } else {
                        actionMode?.invalidate()
                    }
                } else {
                    actionMode?.finish()
                    actionMode = null
                }
            }
        }
        DisposableEffect(state, data) {
            onDispose {
                actionMode?.finish()
            }
        }
    }
}

private class ContextMenuCallback(val state: ContextMenuState) {
    lateinit var data: ContextMenuData
    private val callbacks = mutableMapOf<Int, () -> Unit>()
    fun onCreateActionMode(mode: ActionMode, menu: Menu) {
        data.itemsSeq.forEachIndexed { index, item ->
            menu.add(index, index, index, item.label)
            callbacks[index] = item.onClick
        }
    }

    fun onActionItemClicked(mode: ActionMode, item: MenuItem) {
        state.status = ContextMenuState.Status.Closed
        callbacks[item.itemId]!!()
    }
}

@RequiresApi(23)
private class FloatingTextActionModeCallback(
    private val contentRect: Rect,
    private val callback: ContextMenuCallback
) : ActionMode.Callback2() {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        requireNotNull(item)
        requireNotNull(mode)
        callback.onActionItemClicked(mode, item)
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)
        callback.onCreateActionMode(mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        println("------DESTROY222!")
    }

    override fun onGetContentRect(
        mode: ActionMode?,
        view: View?,
        outRect: android.graphics.Rect?
    ) {
        outRect?.set(
            contentRect.left.toInt(),
            contentRect.top.toInt(),
            contentRect.right.toInt(),
            contentRect.bottom.toInt()
        )
    }
}

private class PrimaryTextActionModeCallback(
    private val callback: ContextMenuCallback
) : ActionMode.Callback {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        requireNotNull(item)
        requireNotNull(mode)
        callback.onActionItemClicked(mode, item)
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)
        callback.onCreateActionMode(mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        println("------DESTROY!")
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(23)
internal object TextToolbarHelperMethods {
    @RequiresApi(23)
    @DoNotInline
    fun startActionMode(
        view: View,
        actionModeCallback: ActionMode.Callback,
        type: Int
    ): ActionMode {
        return view.startActionMode(
            actionModeCallback,
            type
        )
    }

    @RequiresApi(23)
    fun invalidateContentRect(actionMode: ActionMode) {
        actionMode.invalidateContentRect()
    }
}