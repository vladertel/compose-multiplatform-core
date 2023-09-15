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

package androidx.compose.ui.native

import androidx.compose.runtime.DarwinSignposter
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.disposeOnCancellation
import platform.darwin.DISPATCH_BLOCK_INHERIT_QOS_CLASS
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_block_cancel
import platform.darwin.dispatch_block_create
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_t
import platform.darwin.dispatch_time

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
private class TracingDispatchQueueCoroutineDispatcher(
    private val dispatchQueue: dispatch_queue_t,
    private val signposter: DarwinSignposter,
    private val name: String
): CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch_async(dispatchQueue) {
            signposter.trace("dispatch on $name") {
                block.run()
            }
        }
    }

    override fun invokeOnTimeout(
        timeMillis: Long,
        block: Runnable,
        context: CoroutineContext
    ): DisposableHandle {
        val dispatchBlock = dispatch_block_create(DISPATCH_BLOCK_INHERIT_QOS_CLASS) {
            signposter.trace("invokeOnTimeout on $name") {
                block.run()
            }
        }

        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000),
            dispatchQueue,
            dispatchBlock
        )

        return DisposableHandle {
            dispatch_block_cancel(dispatchBlock)
        }
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        val dispatchBlock = dispatch_block_create(DISPATCH_BLOCK_INHERIT_QOS_CLASS) {
            continuation.resume(Unit) {
                it.printStackTrace()
            }
        }

        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000),
            dispatchQueue,
            dispatchBlock
        )

        continuation.disposeOnCancellation {
            dispatch_block_cancel(dispatchBlock)
        }
    }
}

internal fun getMainDispatcher(): CoroutineDispatcher {
    val signposter = DarwinSignposter.scheduling
    return if (signposter.isEnabled) {
        println("Using tracing coroutine dispatcher")

        TracingDispatchQueueCoroutineDispatcher(dispatch_get_main_queue(), signposter, "main")
    } else {
        Dispatchers.Main
    }
}