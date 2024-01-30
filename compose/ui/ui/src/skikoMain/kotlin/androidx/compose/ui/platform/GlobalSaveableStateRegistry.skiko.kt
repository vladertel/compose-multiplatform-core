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

package androidx.compose.ui.platform

import androidx.compose.runtime.saveable.SaveableStateRegistry

private typealias SaveableStateData = Map<String, List<Any?>>

internal class GlobalSaveableStateRegistry(
    val saveableId: String,
) : SaveableStateRegistry by SaveableStateRegistry(
    restoredValues = get(saveableId),
    canBeSaved = {
        // We can save anything in this implementation (SUBJECT TO CHANGE)
        // TODO https://github.com/JetBrains/compose-multiplatform/issues/3480
        true
    }
) {
    fun save() {
        val saved = performSave()
        set(saveableId, saved)
    }

    private companion object {
        private val map = mutableMapOf<String, SaveableStateData>()

        fun get(key: String): SaveableStateData? = map[key]
        fun set(key: String, value: SaveableStateData) { map[key] = value }
    }
}
