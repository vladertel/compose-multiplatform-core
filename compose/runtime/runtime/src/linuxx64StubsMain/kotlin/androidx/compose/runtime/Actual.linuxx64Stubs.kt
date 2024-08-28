/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotContextElement
import kotlin.coroutines.CoroutineContext

internal actual object Trace {
    actual fun beginSection(name: String): Any? = implementedInJetBrainsFork()

    actual fun endSection(token: Any?): Unit = implementedInJetBrainsFork()
}

internal actual typealias CheckResult = androidx.annotation.CheckResult

@Deprecated(
    "MonotonicFrameClocks are not globally applicable across platforms. " +
        "Use an appropriate local clock."
)
actual val DefaultMonotonicFrameClock: MonotonicFrameClock = implementedInJetBrainsFork()

internal actual fun logError(message: String, e: Throwable) {
    implementedInJetBrainsFork()
}

internal actual class ThreadLocal<T> actual constructor(initialValue: () -> T) {
    actual fun get(): T = implementedInJetBrainsFork()
    actual fun set(value: T): Unit = implementedInJetBrainsFork()
    actual fun remove(): Unit = implementedInJetBrainsFork()
}

internal actual class SnapshotThreadLocal<T> actual constructor() {
    actual fun get(): T? = implementedInJetBrainsFork()
    actual fun set(value: T?): Unit = implementedInJetBrainsFork()
}

internal actual class AtomicReference<V> actual constructor(value: V) {
    actual fun get(): V = implementedInJetBrainsFork()
    actual fun set(value: V): Unit = implementedInJetBrainsFork()
    actual fun getAndSet(value: V): V = implementedInJetBrainsFork()
    actual fun compareAndSet(expect: V, newValue: V): Boolean =
            implementedInJetBrainsFork()
}

internal actual class AtomicInt actual constructor(value: Int) {
    actual fun get(): Int = implementedInJetBrainsFork()
    actual fun set(value: Int): Unit = implementedInJetBrainsFork()
    actual fun add(amount: Int): Int = implementedInJetBrainsFork()
    actual fun compareAndSet(expect: Int, newValue: Int): Boolean =
            implementedInJetBrainsFork()
}

internal actual fun identityHashCode(instance: Any?): Int =
        implementedInJetBrainsFork()

internal actual fun ensureMutable(it: Any): Unit = implementedInJetBrainsFork()

internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    actual fun get(): T? = implementedInJetBrainsFork()
}

internal actual fun invokeComposable(
    composer: Composer,
    composable: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()

internal actual fun <T> invokeComposableForResult(
    composer: Composer,
    composable: @Composable () -> T
): T = implementedInJetBrainsFork()

internal actual fun currentThreadId(): Long = implementedInJetBrainsFork()

internal actual fun currentThreadName(): String = implementedInJetBrainsFork()

@OptIn(ExperimentalComposeApi::class)
internal actual class SnapshotContextElementImpl
actual constructor(private val snapshot: Snapshot) : SnapshotContextElement {
    override val key: CoroutineContext.Key<*>
        get() = implementedInJetBrainsFork()
}
