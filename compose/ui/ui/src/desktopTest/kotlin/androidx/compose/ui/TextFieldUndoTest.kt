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

package androidx.compose.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.NoCaretTextField
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import org.junit.Test

class TextFieldUndoTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `TextField undo works after cursor selection and cut`() = runApplicationTest {
        var window: ComposeWindow? = null

        var testedTextValue = ""

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                var text by remember { mutableStateOf(testedTextValue) }

                NoCaretTextField(text, onValueChange = {
                    text = it
                    testedTextValue = it
                })

            }
        }

        awaitIdle()

        window!!.sendKeyEvent(KeyEvent.VK_TAB)

        window!!.sendKeyTypedEvent('a')
        window!!.sendKeyTypedEvent('b')
        window!!.sendKeyTypedEvent('c')
        window!!.sendKeyTypedEvent('d')
        window!!.sendKeyTypedEvent('e')
        window!!.sendKeyTypedEvent('f')


        awaitIdle()

        window!!.sendMouseEvent(MouseEvent.MOUSE_PRESSED, 10, 5, modifiers = MouseEvent.BUTTON1_DOWN_MASK)
        window!!.sendMouseEvent(MouseEvent.MOUSE_DRAGGED, 20, 5, modifiers = MouseEvent.BUTTON1_DOWN_MASK)
        window!!.sendMouseEvent(MouseEvent.MOUSE_RELEASED, 20, 5, modifiers = MouseEvent.BUTTON1_DOWN_MASK)

        awaitIdle()

        window!!.sendKeyEvent(KeyEvent.VK_DELETE)

        awaitIdle()

        assertThat(testedTextValue).isEqualTo("acdef")

        val modifier = if (isMacOs) {
            KeyEvent.META_DOWN_MASK
        } else {
            KeyEvent.CTRL_DOWN_MASK
        }

        window!!.sendKeyEvent(KeyEvent.VK_Z, modifiers = modifier)

        awaitIdle()

        assertThat(testedTextValue).isEqualTo("abcdef")
    }
}