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

import androidx.compose.ui.platform.BackingTextArea
import androidx.compose.ui.text.input.ImeOptions
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLTextAreaElement

class BackingTextAreaTests {
    @Test
    fun backingTextAreaEvents() {
        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = {

            },
            onImeActionPerformed = {

            },
            processKeyboardEvent = {

            }
        )
        var textArea = document.querySelector("textarea")
        assertNull(textArea)

        backingTextArea.register()

        textArea = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(textArea)

        //backingTextArea.dispose()

//        println("DOCUMENT ${document.body?.innerHTML} \n")
//
//        textArea = document.querySelector("textarea")
//        assertNull(textArea)
    }
}