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

package androidx.compose.runtime.collection

import androidx.compose.runtime.implementedInJetBrainsFork

internal actual class IntMap<E>() {
    actual constructor(initialCapacity: Int) : this()
    actual operator fun contains(key: Int): Boolean = implementedInJetBrainsFork()
    actual operator fun get(key: Int): E? = implementedInJetBrainsFork()
    actual fun get(key: Int, valueIfAbsent: E): E = implementedInJetBrainsFork()
    actual operator fun set(key: Int, value: E): Unit = implementedInJetBrainsFork()
    actual fun remove(key: Int): Unit = implementedInJetBrainsFork()
    actual fun clear(): Unit = implementedInJetBrainsFork()
    actual val size: Int
        get() = implementedInJetBrainsFork()
}
