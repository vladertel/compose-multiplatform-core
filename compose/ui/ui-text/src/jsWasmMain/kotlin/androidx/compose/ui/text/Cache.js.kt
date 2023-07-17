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

package androidx.compose.ui.text

internal actual typealias WeakKeysCache<K, V> = WeakHashMap<K, V>

internal class WeakHashMap<K : Any, V> : Cache<K, V> {
    private val cache = HashMap<Key<K>, V>()

    override fun get(key: K, loader: (K) -> V): V {
        clean()
        return cache.getOrPut(Key(key)) {
            println("Load - ${key.hashCode()}")
            loader(key)
        }
    }

    private fun clean() {
        cache.keys
            .filter { !it.isAvailable }
            .forEach {
                cache.remove(it)
            }
    }

    private class Key<K : Any>(key: K) {
        @OptIn(InternalTextApi::class)
        private val ref = newWeakRef(key.toJsReferenceType())
        private val hash: Int = key.hashCode()

        val isAvailable get() = ref.deref() != null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            other as Key<*>
            return ref.deref() == other.ref.deref()
        }

        override fun hashCode(): Int = hash
    }
}


@InternalTextApi
expect class JsReferenceType

private external interface WeakRef {
    fun deref(): Any?
}

@OptIn(InternalTextApi::class)
@Suppress("UnsafeCastFromDynamic")
private fun newWeakRef(obj: JsReferenceType): WeakRef = js("new WeakRef(obj)")
@OptIn(InternalTextApi::class)
internal expect fun Any.toJsReferenceType(): JsReferenceType

