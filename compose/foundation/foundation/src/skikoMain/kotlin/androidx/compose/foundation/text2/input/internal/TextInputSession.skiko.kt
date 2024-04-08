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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.TextFieldStateAdapter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue

// TODO(https://youtrack.jetbrains.com/issue/COMPOSE-733/Merge-1.6.-Apply-changes-for-the-new-text-input) implement
internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    imeOptions: ImeOptions,
    onImeAction: ((ImeAction) -> Unit)?
): Nothing {
    val editProcessor = EditProcessor()
    fun onEditCommand(commands: List<EditCommand>) {
        editProcessor.reset(
            value = with(state.text) {
                TextFieldValue(
                    text = toString(),
                    selection = selectionInChars,
                    composition = compositionInChars
                )
            },
            textInputSession = null
        )

        val newValue = editProcessor.apply(commands)

        state.replaceAll(newValue.text)
        state.editUntransformedTextAsUser {
            val untransformedSelection = state.mapFromTransformed(newValue.selection)
            setSelection(untransformedSelection.start, untransformedSelection.end)

            val composition = newValue.composition
            if (composition == null) {
                commitComposition()
            } else {
                val untransformedComposition = state.mapFromTransformed(composition)
                setComposition(untransformedComposition.start, untransformedComposition.end)
            }
        }
    }

    startInputMethod(
        SkikoPlatformTextInputMethodRequest(
            state = TransformedTextFieldStateAdapter(state),
            imeOptions = imeOptions,
            onEditCommand = ::onEditCommand,
            onImeAction = onImeAction
        )
    )
}


private class TransformedTextFieldStateAdapter(
    val state: TransformedTextFieldState
) : TextFieldStateAdapter {

    override val text: CharSequence
        get() = state.text

    override val selection: TextRange
        get() = state.text.selectionInChars

    override val composition: TextRange?
        get() = state.text.compositionInChars

}

private data class SkikoPlatformTextInputMethodRequest(
    override val state: TextFieldStateAdapter,
    override val imeOptions: ImeOptions,
    override val onEditCommand: (List<EditCommand>) -> Unit,
    override val onImeAction: ((ImeAction) -> Unit)?
): PlatformTextInputMethodRequest