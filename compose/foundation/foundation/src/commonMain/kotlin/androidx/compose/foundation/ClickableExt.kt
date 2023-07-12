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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role

internal fun Modifier.focusableClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    val focusRequester = remember { FocusRequester() }
    this
        .focusRequester(focusRequester)
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .then(
            ClickableElement(
                interactionSource,
                focusRequester,
                enabled,
                onClickLabel,
                role,
                onClick
            )
        )
}

internal fun Modifier.focusableCombinedClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role? = null,
    onClick: () -> Unit,
    onLongClickLabel: String?,
    onLongClick: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?
) = composed {
    val focusRequester = remember { FocusRequester() }
    this
        .focusRequester(focusRequester)
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .then(
            CombinedClickableElement(
                interactionSource,
                focusRequester,
                enabled,
                onClickLabel,
                role,
                onClick,
                onLongClickLabel,
                onLongClick,
                onDoubleClick
            )
        )
}