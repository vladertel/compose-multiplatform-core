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

package bugs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

//https://github.com/JetBrains/compose-multiplatform/issues/4206
val TextFieldCrashVisualTransformation = Screen.Example(
    "TextFieldCrashVisualTransformation"
) {
    val focusRequester = remember { FocusRequester() }

    Column {
        Text("Click to TextField bellow to reproduce crash")
        OutlinedTextField(
            value = "1000",
            onValueChange = {},
            visualTransformation = CurrencyVisualTransformation,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

fun formatToCurrency(number: String): String = number.reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

val CurrencyVisualTransformation = VisualTransformation { text ->

    var processedText = if(text.isNotEmpty() && text.all { it.isDigit() }) {
        formatToCurrency(text.toString())
    } else text

    val out = AnnotatedString("$" + processedText)

    TransformedText(out, OffsetMapping.Identity)
}
