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
import kotlinx.atomicfu.*
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_signal
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_get_qos_class_np
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import platform.posix.pthread_mutexattr_destroy
import platform.posix.pthread_mutexattr_init
import platform.posix.pthread_mutexattr_settype
import platform.posix.pthread_mutexattr_t
import platform.posix.pthread_override_qos_class_end_np
import platform.posix.pthread_override_qos_class_start_np
import platform.posix.pthread_override_t
import platform.posix.pthread_self
import platform.posix.pthread_t
import platform.posix.qos_class_self
import platform.posix.qos_class_t

import platform.posix.PTHREAD_MUTEX_ERRORCHECK


@ThreadLocal
var currentThreadId = 0L


internal actual class SynchronizedObject actual constructor() {

    companion object {
        private const val NO_OWNER = 0L
    }

    private val owner: AtomicLong = atomic(NO_OWNER)
    private var reEnterCount: Int = 0
    private val waiters: AtomicInt = atomic(0)

    private val manager: Manager by lazy { Manager() }
    private val monitor: NativeMonitor get() = manager.monitor


    fun lock() {
        var self = currentThreadId
        if (self == 0L) {
            currentThreadId = pthread_self().toLong()
            self = currentThreadId
        }
        if (owner.value == self) {
            reEnterCount += 1
        } else if (waiters.incrementAndGet() > 1) {
            waitForUnlockAndLock(self)
        } else {
            if (!owner.compareAndSet(NO_OWNER, self)) {
                waitForUnlockAndLock(self)
            }
        }
    }

    private fun waitForUnlockAndLock(self: Long) {
        withMonitor(monitor) {
            while (!owner.compareAndSet(NO_OWNER, self)) {
                manager.donateQos(owner.value)
                monitor.wait()
                manager.clearDonation()
            }
        }
    }

    fun unlock() {
        require (owner.value == currentThreadId)
        if (reEnterCount > 0) {
            reEnterCount -= 1
        } else {
            owner.value = NO_OWNER
            if (waiters.decrementAndGet() > 0) {
                withMonitor(monitor) {
                    // We expect the highest priority thread to be woken up, but this should work
                    // in any case.
                    monitor.notify()
                }
            }
        }
    }

    private inline fun withMonitor(monitor: NativeMonitor, block: NativeMonitor.() -> Unit) {
        monitor.run {
            enter()
            return try {
                block()
            } finally {
                exit()
            }
        }
    }

    private class Manager {
        val monitor: NativeMonitor = NativeMonitor()
        val cleaner = createCleaner(monitor, NativeMonitor::dispose)
        var qosOverride: pthread_override_t? = null
        var qosOverrideQosClass: UInt = 0U

        fun donateQos(lockOwner: Long) {
            if (lockOwner == NO_OWNER) {
                return
            }
            val ourQosClass = qos_class_self() as UInt
            // Set up a new override if required:
            if (qosOverride != null) {
                // There is an existing override, but we need to go higher.
                if (ourQosClass > qosOverrideQosClass) {
                    pthread_override_qos_class_end_np(qosOverride)
                    qosOverride = pthread_override_qos_class_start_np(lockOwner.toCPointer(), qos_class_self(), 0)
                    qosOverrideQosClass = ourQosClass
                }
            } else {
                // No existing override, check if we need to set one up.
                memScoped {
                    val lockOwnerQosClass = alloc<UIntVar>()
                    val lockOwnerRelPrio = alloc<IntVar>()
                    pthread_get_qos_class_np(lockOwner.toCPointer(), lockOwnerQosClass.ptr, lockOwnerRelPrio.ptr)
                    if (ourQosClass > lockOwnerQosClass.value) {
                        qosOverride = pthread_override_qos_class_start_np(lockOwner.toCPointer(), qos_class_self(), 0)
                        qosOverrideQosClass = ourQosClass
                    }
                }
            }
        }

        fun clearDonation() {
            if (qosOverride != null) {
                pthread_override_qos_class_end_np(qosOverride)
                qosOverride = null
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private class NativeMonitor {
        private val arena: Arena = Arena()
        private val cond: pthread_cond_t = arena.alloc()
        private val mutex: pthread_mutex_t = arena.alloc()
        private val attr: pthread_mutexattr_t = arena.alloc()

        init {
            require (pthread_cond_init(cond.ptr, null) == 0)
            require(pthread_mutexattr_init(attr.ptr) == 0)
            require (pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_ERRORCHECK) == 0)
            require(pthread_mutex_init(mutex.ptr, attr.ptr) == 0)
        }

        fun enter() = require(pthread_mutex_lock(mutex.ptr) == 0)

        fun exit() = require(pthread_mutex_unlock(mutex.ptr) == 0)

        fun wait() = require(pthread_cond_wait(cond.ptr, mutex.ptr) == 0)

        fun notify() = require (pthread_cond_signal(cond.ptr) == 0)

        fun dispose() {
            pthread_cond_destroy(cond.ptr)
            pthread_mutex_destroy(mutex.ptr)
            pthread_mutexattr_destroy(attr.ptr)
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
