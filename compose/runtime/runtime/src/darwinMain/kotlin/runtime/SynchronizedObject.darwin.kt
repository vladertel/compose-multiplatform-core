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

package androidx.compose.runtime

import kotlin.native.ref.createCleaner
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.value
import platform.darwin.os_unfair_lock_lock
import platform.darwin.os_unfair_lock_t
import platform.darwin.os_unfair_lock_trylock
import platform.darwin.os_unfair_lock_unlock
import platform.darwin.os_unfair_lock
import platform.darwin.os_unfair_lock_s
import platform.posix.pthread_cond_t
import platform.posix.pthread_self
import platform.posix.pthread_t

internal actual class SynchronizedObject actual constructor() {

    private var owner: pthread_t? = null  // writes are guarded by monitor
    private var reEnterCount = 0  // only accessed by owner
    private val monitorWrapper = MonitorWrapper()
    private val monitor: NativeMonitor get() = monitorWrapper.monitor

    fun lock() {
        if (owner == pthread_self()) {
            reEnterCount += 1
        } else {
            monitor.enter()
            owner = pthread_self()
        }
    }

    fun unlock() {
        if (reEnterCount > 0) {
            reEnterCount -= 1
        } else {
            owner = null
            monitor.exit()
        }
    }

    private class MonitorWrapper {
        val monitor: NativeMonitor = NativeMonitor()
        val cleaner = createCleaner(monitor, NativeMonitor::dispose)
    }


    @OptIn(ExperimentalForeignApi::class)
    private class NativeMonitor {
        private val arena: Arena = Arena()

        private val lock: os_unfair_lock_s = arena.alloc()

        init {
            // Can't access OS_UNFAIR_LOCK_OPAQUE.
            lock._os_unfair_lock_opaque = 0u
        }

        fun enter() {
            if (!os_unfair_lock_trylock(lock.ptr)) { // Avoid a native blocking call if possible.
                os_unfair_lock_lock(lock.ptr)
            }
        }

        fun exit() = os_unfair_lock_unlock(lock.ptr)

        fun dispose() {
            arena.clear()
        }
    }
}

@PublishedApi
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    lock.run {
        lock()
        return try {
            block()
        } finally {
            unlock()
        }
    }
}