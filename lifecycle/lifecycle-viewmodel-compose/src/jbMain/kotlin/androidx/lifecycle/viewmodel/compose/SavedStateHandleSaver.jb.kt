/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.Bundle
import androidx.savedstate.bundleOf

/**
 * Inter-opt between [SavedStateHandle] and [Saver] so that any state holder that is
 * being saved via [rememberSaveable] with a custom [Saver] can also be saved with
 * [SavedStateHandle].
 *
 * The returned state [T] should be the only way that a value is saved or restored from the
 * [SavedStateHandle] with the given [key].
 *
 * Using the same key again with another [SavedStateHandle] method is not supported, as values
 * won't cross-set or communicate updates.
 *
 * @sample androidx.lifecycle.viewmodel.compose.samples.SnapshotStateViewModel
 */
@SavedStateHandleSaveableApi
actual fun <T : Any> SavedStateHandle.saveable(
    key: String,
    saver: Saver<T, out Any>,
    init: () -> T
): T {
    @Suppress("UNCHECKED_CAST")
    saver as Saver<T, Any>
    // value is restored using the SavedStateHandle or created via [init] lambda
    val value = get<Bundle?>(key)?.getUnsafe("value")?.let(saver::restore) ?: init()

    // Hook up saving the state to the SavedStateHandle
    setSavedStateProvider(key) {
        bundleOf("value" to with(saver) {
            SaverScope(SavedStateHandle.Companion::validateValue).save(value)
        })
    }
    return value
}
