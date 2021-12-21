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

package androidx.compose.ui.platform

import kotlinx.coroutines.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal val EmptyDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = Unit
}

/**
 * Dispatcher with the ability to immediately perform (flush) all pending tasks.
 * Without a flush all tasks are dispatched in the dispatcher provided by [scope]
 */
@OptIn(InternalCoroutinesApi::class)
internal class FlushCoroutineDispatcher(
    scope: CoroutineScope
) : CoroutineDispatcher(), Delay  {
    // Dispatcher should always be alive, even if Job is cancelled. Otherwise coroutines which
    // use this dispatcher won't be properly cancelled.
    // TODO replace it by scope.coroutineContext[Dispatcher] when it will be no longer experimental
    private val scope = CoroutineScope(scope.coroutineContext.minusKey(Job))

    private val tasks = mutableSetOf<Runnable>()
    private val tasksCopy = mutableSetOf<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasks) {
            tasks.add(block)
        }
        scope.launch {
            val isTaskAlive = synchronized(tasks) {
                tasks.remove(block)
            }
            if (isTaskAlive) {
                block.run()
            }
        }
    }

    fun hasTasks() = synchronized(tasks) {
        tasks.isNotEmpty()
    }

    fun flush() {
        synchronized(tasks) {
            tasksCopy.addAll(tasks)
            tasks.clear()
        }

        tasksCopy.forEach(Runnable::run)
        tasksCopy.clear()
    }

    @Suppress("INVISIBLE_MEMBER")
    private fun getDelayForScope() = scope.coroutineContext.get(ContinuationInterceptor) as? Delay ?: DefaultDelay

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        getDelayForScope().scheduleResumeAfterDelay(timeMillis, continuation)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        return getDelayForScope().invokeOnTimeout(timeMillis, block, context)
    }
}
