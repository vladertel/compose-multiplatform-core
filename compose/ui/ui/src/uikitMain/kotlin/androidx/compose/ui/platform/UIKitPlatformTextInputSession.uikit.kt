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

package androidx.compose.ui.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

internal class UIKitPlatformTextInputSession(
    private val textInputService: UIKitTextInputService,
    private val coroutineScope: CoroutineScope
) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {

//    private val methodSessionMutex = SessionMutex<Unit>()

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        suspendCancellableCoroutine<Nothing> { continuation ->
            // Show the keyboard and ask the IMM to restart input.
            textInputService.startInput()

            // The cleanup needs to be executed synchronously, otherwise the stopInput call
            // might come too late and end up overriding the next field's startInput. This
            // is prevented by the queuing in TextInputService, but can still happen when
            // focus transfers from a compose text field to a view-based text field
            // (EditText).
            continuation.invokeOnCancellation {
                // If this session was cancelled because another session was requested, this
                // call will be a noop.
                textInputService.stopInput()
            }
        }
    }
}