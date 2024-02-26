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

package androidx.compose.ui.util

@ExperimentalComposeApi
fun enableTraceOSLog() {
    if (traceImpl == null) {
        traceImpl = CMPOSLogger(categoryName = "androidx.compose.ui")
    }
}

private var traceImpl: CMPOSLogger? = null

actual inline fun <T> trace(sectionName: String, block: () -> T): T {
    val interval = traceImpl?.beginIntervalNamed(sectionName)
    try {
        return block()
    } finally {
        interval?.let {
            traceImpl?.endInterval(it)
        }
    }
}