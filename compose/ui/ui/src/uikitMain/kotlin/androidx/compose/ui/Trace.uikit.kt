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

package androidx.compose.ui

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.uikit.utils.CMPOSLogger
import androidx.compose.ui.uikit.utils.CMPOSLoggerInterval

@ExperimentalComposeApi
fun enableTraceOSLog() {
    if (traceImpl == null) {
        traceImpl = CMPOSLogger(categoryName = "androidx.compose.ui")
    }
}

private var traceImpl: CMPOSLogger? = null

// Verbatim of androidx.compose.runtime.Trace
internal object Trace {
    /**
     * Writes a trace message to indicate that a given section of code has begun.
     * This call must be followed by a corresponding call to [endSection] on the same thread.
     *
     * @return An arbitrary token which will be supplied to the corresponding call
     * to [endSection]. May be null.
     */
    fun beginSection(name: String): CMPOSLoggerInterval? {
        return traceImpl?.beginIntervalNamed(name)
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended.
     * This call must be preceded by a corresponding call to [beginSection].
     * Calling this method will mark the end of the most recently begun section of code, so care
     * must be taken to ensure that `beginSection` / `endSection` pairs are properly nested and
     * called from the same thread.
     *
     * @param interval The instance returned from the corresponding call to [beginSection].
     */
    fun endSection(interval: CMPOSLoggerInterval) {
        traceImpl?.endInterval(interval)
    }
}

/**
 * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [sectionName])
 * and [Trace.endSection].
 */
internal inline fun <T> trace(sectionName: String, block: () -> T): T {
    val token = Trace.beginSection(sectionName)
    try {
        return block()
    } finally {
        token?.let {
            Trace.endSection(it)
        }
    }
}