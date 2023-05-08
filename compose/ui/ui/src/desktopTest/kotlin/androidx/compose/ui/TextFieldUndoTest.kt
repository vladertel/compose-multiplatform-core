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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import org.junit.Test

@Composable
fun NoCaretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(Color.Unspecified),
        visualTransformation = visualTransformation,
    )
}

class TextFieldUndoTest {
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