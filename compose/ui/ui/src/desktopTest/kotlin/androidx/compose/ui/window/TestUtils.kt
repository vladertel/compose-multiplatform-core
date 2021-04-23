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

package androidx.compose.ui.window

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assume.assumeFalse
import java.awt.GraphicsEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runApplicationTest(
    /**
     * Use delay(500) additionally to `yield` in `await*` functions
     *
     * Set this property only if you sure that you can't easily make the test deterministic
     * (non-flaky).
     */
    useDelay: Boolean = false,
    body: suspend WindowTestScope.() -> Unit
) {
    assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

    runBlocking(Dispatchers.Swing) {
        withTimeout(30000) {
            WindowTestScope(this, useDelay).body()
        }
    }
}

internal class WindowTestScope(
    private val scope: CoroutineScope,
    private val useDelay: Boolean
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    suspend fun awaitIdle() {
        if (useDelay) {
            delay(500)
        }
        // TODO(demin): It seems this not-so-good synchronization
        //  doesn't cause flakiness in our window tests.
        //  But more robust solution will be to use something like
        //  TestCoroutineDispatcher/FlushCoroutineDispatcher (but we can't use it in a pure form,
        //  because there are Swing/system events that we don't control).
        // Most of the work usually is done after the first yield(), almost all of the work -
        // after fourth yield()
        repeat(100) {
            yield()
        }
    }
}

private val os = System.getProperty("os.name").lowercase()
internal val isLinux = os.startsWith("linux")
internal val isWindows = os.startsWith("win")
internal val isMacOs = os.startsWith("mac")