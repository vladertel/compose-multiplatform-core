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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyMapping
import androidx.compose.foundation.text.createMacosDefaultKeyMapping
import androidx.compose.foundation.text.defaultSkikoKeyMapping
import androidx.compose.foundation.text.overriddenDefaultKeyMapping
import androidx.compose.foundation.text.selection.config.DefaultKeyboardActions
import androidx.compose.foundation.text.selection.config.KeyboardActions
import androidx.compose.foundation.text.selection.config.MacosKeyboardActions
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
internal class SelectionTests(private val keyboardActions: KeyboardActions, private val keyMapping: KeyMapping): KeyboardActions by keyboardActions {

    @ExperimentalTestApi
    private val composeTest = SkikoComposeUiTest()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} shortcuts")
        fun initParams() = arrayOf(
            arrayOf(DefaultKeyboardActions, defaultSkikoKeyMapping),
            arrayOf(MacosKeyboardActions, createMacosDefaultKeyMapping())
        )
    }

    private fun setPlatformDefaultKeyMapping(value: KeyMapping) {
        overriddenDefaultKeyMapping = value
    }

    @ExperimentalTestApi
    fun SemanticsNodeInteraction.waitAndCheck(check: () -> Unit): SemanticsNodeInteraction {
        composeTest.waitForIdle()
        check()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    private fun textFieldSemanticInteraction(initialValue: String = "", semanticNodeContext: SemanticsNodeInteraction.(state: MutableState<TextFieldValue>) -> SemanticsNodeInteraction) =
    composeTest.runTest {
        setPlatformDefaultKeyMapping(keyMapping)
        val state = mutableStateOf(TextFieldValue(initialValue))

        composeTest.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        composeTest.waitForIdle()
        val textField = composeTest.onNodeWithTag("textField")
        textField.performMouseInput {
            click(Offset(0f, 0f))
        }

        composeTest.waitForIdle()
        textField.assertIsFocused()

        assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        semanticNodeContext.invoke(textField, state)
    }


    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectLineStart() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput { this.selectLineStart() }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 7))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectTextStart() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            performKeyInput { this.selectTextStart() }
            .waitAndCheck { assertThat(state.value.selection).isEqualTo(TextRange(8, 0)) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectTextEnd() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput { this.selectTextEnd() }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 34))
             }
        }
    }
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectLineEnd() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput { this.selectLineEnd() }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(8, 13))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteAll() {
        textFieldSemanticInteraction("") { state ->
            performKeyInput{ this.deleteAll() }.waitAndCheck { assertThat(state.value.text).isEqualTo("") }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectAll() {
        textFieldSemanticInteraction("Select this text") { state ->
            performKeyInput { this.selectAll() }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(0, 16))
            }
            .performKeyInput { keyDown(Key.Delete) }
            .waitAndCheck {
                assertThat(state.value.selection).isEqualTo(TextRange(0, 0))
                assertThat(state.value.text).isEqualTo("")
            }
        }
    }
}
