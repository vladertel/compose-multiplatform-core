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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.text.AnnotatedString
import kotlinx.browser.document
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event

@Composable
@NonRestartableComposable
internal actual inline fun rememberClipboardEventsHandler(
    textFieldSelectionManager: TextFieldSelectionManager,
    isFocused: Boolean
) {
    if (isFocused) {
        println("setup rememberClipboardEventsHandler")

        DisposableEffect(Unit) {
            val onCopy: (Event) -> Unit = { event  ->
                println("onCopy $event")
                val textToCopy = textFieldSelectionManager.copyWithResult()
                if (textToCopy != null && event is ClipboardEvent) {
                    event.clipboardData?.setData("text/plain", textToCopy)
                    event.preventDefault()
                }
            }
            document.addEventListener("copy", onCopy)

            val onPaste: (Event) -> Unit = { event ->
                println("onPaste $event")
                if (event is ClipboardEvent) {
                    val textToPaste = event.clipboardData?.getData("text/plain") ?: ""
                    event.preventDefault()
                    textFieldSelectionManager.paste(AnnotatedString(textToPaste))
                }
            }
            document.addEventListener("paste", onPaste)

            return@DisposableEffect onDispose {
                document.removeEventListener("copy", onCopy)
                document.removeEventListener("paste", onPaste)
            }
        }
    }
}