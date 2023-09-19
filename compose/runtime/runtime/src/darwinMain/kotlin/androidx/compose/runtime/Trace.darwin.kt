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

package androidx.compose.runtime

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.darwin.OS_SIGNPOST_INTERVAL_BEGIN
import platform.darwin.OS_SIGNPOST_INTERVAL_END
import platform.darwin.__dso_handle
import platform.darwin._os_signpost_emit_with_name_impl
import platform.darwin.os_log_create
import platform.darwin.os_signpost_enabled
import platform.darwin.os_signpost_id_generate
import platform.darwin.os_signpost_id_t
import platform.darwin.os_signpost_type_t

data class DarwinSignpostInterval(
    val name: String,
    val id: os_signpost_id_t
)

class DarwinSignposter(category: String) {
    private val log = os_log_create(subsystem = "androidx.compose", category = category)
    private val buf = UByteArray(1024)
    private fun event(id: os_signpost_id_t, name: String, type: os_signpost_type_t) {
        buf.usePinned { pinned ->
            _os_signpost_emit_with_name_impl(
                __dso_handle.ptr,
                log,
                type,
                id,
                name,
                format = "",
                buf = pinned.addressOf(0),
                size = 1024u
            )
        }
    }

    fun begin(name: String) = if (os_signpost_enabled(log)) {
        os_signpost_id_generate(log).let { id ->
            event(id, name, OS_SIGNPOST_INTERVAL_BEGIN)

            DarwinSignpostInterval(name, id)
        }
    } else {
        null
    }

    fun end(interval: DarwinSignpostInterval) {
        event(interval.id, interval.name, OS_SIGNPOST_INTERVAL_END)
    }

    inline fun <T> trace(name: String, block: () -> T): T {
        val interval = begin(name)
        try {
            return block()
        } finally {
            interval?.let { end(it) }
        }
    }

    companion object {
        val runtime = DarwinSignposter(category = "runtime")
    }
}

internal actual object Trace {
    /**
     * Writes a trace message to indicate that a given section of code has begun.
     * This call must be followed by a corresponding call to [endSection] on the same thread.
     *
     * @return An arbitrary token which will be supplied to the corresponding call
     * to [endSection]. May be null.
     */
    actual fun beginSection(name: String): Any? =
        DarwinSignposter.runtime.begin(name)

    /**
     * Writes a trace message to indicate that a given section of code has ended.
     * This call must be preceded by a corresponding call to [beginSection].
     * Calling this method will mark the end of the most recently begun section of code, so care
     * must be taken to ensure that `beginSection` / `endSection` pairs are properly nested and
     * called from the same thread.
     *
     * @param token The instance returned from the corresponding call to [beginSection].
     */
    actual fun endSection(token: Any?) {
        if (token != null) {
            DarwinSignposter.runtime.end(token as DarwinSignpostInterval)
        }
    }
}