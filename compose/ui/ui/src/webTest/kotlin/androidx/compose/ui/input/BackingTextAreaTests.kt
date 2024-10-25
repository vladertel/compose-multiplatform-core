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

package androidx.compose.ui.input

import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.platform.BackingTextArea
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.SetComposingTextCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

class BackingTextAreaTests {
    lateinit var lastEditCommand: List<EditCommand>

    @Test
    fun backingTextAreaEvents() {
        val processedKeys = mutableListOf<String>()

        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = { command ->
                lastEditCommand = command
            },
            onImeActionPerformed = {},
            processKeyboardEvent = { evt ->
                processedKeys.add(evt.key)
            }
        )
        var textArea = document.querySelector("textarea")
        assertNull(textArea)

        backingTextArea.register()

        textArea = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(textArea)

        with (textArea) {
            dispatchEvent(keyEvent("H"))
            dispatchEvent(keyEvent("E"))
            dispatchEvent(keyEvent("L"))
            dispatchEvent(keyEvent("L"))
            dispatchEvent(keyEvent("O"))
        }

        assertEquals("H:E:L:L:O", processedKeys.joinToString(":"))

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "insertText", data = "Bonjour")))

        assertEquals(listOf(CommitTextCommand("Bonjour", 1)), lastEditCommand)

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "deleteContentBackward", data = "")))

        assertEquals("H:E:L:L:O:Backspace", processedKeys.joinToString(":"))

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "insertCompositionText", data = "Servus")))

        assertEquals(listOf(SetComposingTextCommand("Servus", 1)), lastEditCommand)

        backingTextArea.dispose()

        textArea = document.querySelector("textarea")
        assertNull(textArea)
    }
}