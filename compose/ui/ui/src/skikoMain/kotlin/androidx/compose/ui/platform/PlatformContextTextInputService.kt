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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession


/**
 * Platform specific text input service.
 *
 * This is a non-deprecated version of [PlatformTextInputService], which is needed because we can't
 * expose deprecated APIs in [PlatformContext].
 */
interface PlatformContextTextInputService {
    /**
     * Start text input session for given client.
     *
     * @see TextInputService.startInput
     */
    fun startInput(
        value: TextFieldStateAdapter,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: ((ImeAction) -> Unit)?
    )

    /**
     * Restart input and show the keyboard. This should only be called when starting a new
     * `PlatformTextInputModifierNode.textInputSession`.
     *
     * @see TextInputService.startInput
     */
    fun startInput() {}

    /**
     * Stop text input session.
     *
     * @see TextInputService.stopInput
     */
    fun stopInput()

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     *
     * @see TextInputService.showSoftwareKeyboard
     */
    fun showSoftwareKeyboard()

    /**
     * Hide software keyboard
     *
     * @see TextInputService.hideSoftwareKeyboard
     */
    fun hideSoftwareKeyboard()

    /**
     * Notify the new editor model to IME.
     *
     * @see TextInputSession.updateState
     */
    fun updateState(oldValue: TextFieldStateAdapter?, newValue: TextFieldStateAdapter)

    /**
     * Notify the focused rectangle to the system.
     *
     * The system can ignore this information or use it to for additional functionality.
     *
     * For example, desktop systems show a popup near the focused input area (for some languages).
     */
    // TODO(b/262648050) Try to find a better API.
    fun notifyFocusedRect(rect: Rect) {
    }

    /**
     * Notify the input service of layout and position changes.
     *
     * @see TextInputSession.updateTextLayoutResult
     */
    fun updateTextLayoutResult(
        textFieldValue: TextFieldStateAdapter,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
    }
}

@Suppress("DEPRECATION")
internal fun PlatformContextTextInputService.asPlatformTextInputService() = object :
    PlatformTextInputService {
    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        this@asPlatformTextInputService.startInput(
            value = value.asTextFieldStateAdapter(),
            imeOptions = imeOptions,
            onEditCommand = onEditCommand,
            onImeActionPerformed = onImeActionPerformed
        )
    }

    override fun startInput() = this@asPlatformTextInputService.startInput()

    override fun stopInput() = this@asPlatformTextInputService.stopInput()

    override fun showSoftwareKeyboard() = this@asPlatformTextInputService.showSoftwareKeyboard()

    override fun hideSoftwareKeyboard() = this@asPlatformTextInputService.hideSoftwareKeyboard()

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        this@asPlatformTextInputService.updateState(
            oldValue = oldValue?.asTextFieldStateAdapter(),
            newValue = newValue.asTextFieldStateAdapter()
        )
    }

    override fun notifyFocusedRect(rect: Rect) {
        this@asPlatformTextInputService.notifyFocusedRect(rect)
    }

    override fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
        this@asPlatformTextInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue.asTextFieldStateAdapter(),
            offsetMapping = offsetMapping,
            textLayoutResult = textLayoutResult,
            textFieldToRootTransform = textFieldToRootTransform,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )
    }
}
