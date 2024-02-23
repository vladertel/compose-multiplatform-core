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

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.savedstate.Bundle
import androidx.savedstate.SavedStateRegistry
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.StateFlow

actual class SavedStateHandle {
    actual constructor(initialState: Map<String, Any?>) {
        TODO("Not yet implemented")
    }

    actual constructor() {
        TODO("Not yet implemented")
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual fun savedStateProvider(): SavedStateRegistry.SavedStateProvider {
        TODO("Not yet implemented")
    }

    @MainThread
    actual operator fun contains(key: String): Boolean {
        TODO("Not yet implemented")
    }

    @MainThread
    actual fun <T> getStateFlow(
        key: String,
        initialValue: T
    ): StateFlow<T> {
        TODO("Not yet implemented")
    }

    @MainThread
    actual fun keys(): Set<String> {
        TODO("Not yet implemented")
    }

    @MainThread
    actual operator fun <T> get(key: String): T? {
        TODO("Not yet implemented")
    }

    @MainThread
    actual operator fun <T> set(key: String, value: T?) {
    }

    @MainThread
    actual fun <T> remove(key: String): T? {
        TODO("Not yet implemented")
    }

    @MainThread
    actual fun setSavedStateProvider(
        key: String,
        provider: SavedStateRegistry.SavedStateProvider
    ) {
    }

    @MainThread
    actual fun clearSavedStateProvider(key: String) {
    }

    actual companion object {
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        actual fun createHandle(
            restoredState: Bundle?,
            defaultState: Bundle?
        ): SavedStateHandle {
            TODO("Not yet implemented")
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        actual fun validateValue(value: Any?): Boolean {
            TODO("Not yet implemented")
        }

    }

}