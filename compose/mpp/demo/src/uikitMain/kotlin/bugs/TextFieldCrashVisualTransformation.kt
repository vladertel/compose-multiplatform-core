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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sign
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCurrencySymbol
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

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
            visualTransformation = vs,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private val vs = StyleNumberVisualTransformation(
    decimalFormat = getDefaultLocaleDecimalFormatByCurrencyCode("USD"),
    transformText =  { AnnotatedString(it) },
)

@Stable
internal class StyleNumberVisualTransformation(
    private val decimalFormat: DecimalFormat,
    private val transformText: StyleNumberVisualTransformationScope.(formattedText: String) -> AnnotatedString
) : VisualTransformation {

    interface StyleNumberVisualTransformationScope {

        val integerPartIndices: IntRange

        val decimalSeparatorIndex: Int

        val fractionPartIndices: IntRange

        val extraSymbolIndices: IntRange
    }

    private class NumberOffsetMapping(
        private val originalToTransformedOffsets: IntArray,
        private val transformedToOriginalOffsets: IntArray
    ) : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int {
            return originalToTransformedOffsets[offset]
        }

        override fun transformedToOriginal(offset: Int): Int {
            return transformedToOriginalOffsets[offset]
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val number = text.text.toDoubleOrNull()
            ?: return TransformedText(text, OffsetMapping.Identity)

        val decimalSeparatorIndex = text.text.indexOf('.')

        val decimalSeparatorLength = when (decimalSeparatorIndex == -1) {
            true -> 0
            false -> 1
        }

        val integerPartIndices = when (decimalSeparatorIndex == -1) {
            true -> 0.rangeTo(text.text.lastIndex)
            false -> 0 until decimalSeparatorIndex
        }

        val integerPartLength = integerPartIndices.length

        val fractionPartIndices = when (decimalSeparatorIndex) {
            -1, text.text.lastIndex -> IntRange.EMPTY
            else -> (decimalSeparatorIndex + 1).rangeTo(text.text.lastIndex)
        }

        val fractionPartLength = fractionPartIndices.length

        val prefix =
            when (number.sign) {
                0.0, 1.0 -> decimalFormat.positivePrefix
                else -> decimalFormat.negativePrefix
            }

        val suffix =
            when (number.sign) {
                0.0, 1.0 -> decimalFormat.positiveSuffix
                else -> decimalFormat.negativeSuffix
            }

        val groupingSeparatorCount =
            when (integerPartLength % decimalFormat.groupingSize == 0) {
                true -> integerPartLength / decimalFormat.groupingSize - 1
                false -> integerPartLength / decimalFormat.groupingSize
            }

        val transformedTextLength =
            prefix.length
                .plus(integerPartLength)
                .plus(groupingSeparatorCount)
                .plus(decimalSeparatorLength)
                .plus(fractionPartLength)
                .plus(suffix.length)

        val transformedTextBuilder = StringBuilder(transformedTextLength)

        val originalToTransformedOffsets = IntArray(text.text.length + 1)
        val transformedToOriginalOffsets = IntArray(transformedTextLength + 1)

        var transformedTextOffset = 0

        transformedTextBuilder.append(prefix)

        repeat(prefix.length) {
            transformedToOriginalOffsets[transformedTextOffset++] = 0
        }

        originalToTransformedOffsets[0] =
            transformedTextOffset /* can be zero if cursor need to be before prefix */

        transformedToOriginalOffsets[transformedTextOffset] = 0

        transformedTextOffset += 1

        integerPartIndices.forEach { index ->
            val reversedIndex = integerPartIndices.last - index

            transformedTextBuilder.append(text.text[index])

            originalToTransformedOffsets[index + 1] = transformedTextOffset
            transformedToOriginalOffsets[transformedTextOffset] = index + 1

            transformedTextOffset += 1

            if (reversedIndex != 0 && reversedIndex % decimalFormat.groupingSize == 0) {
                transformedTextBuilder.append(decimalFormat.decimalFormatSymbols.groupingSeparator)
                transformedToOriginalOffsets[transformedTextOffset++] = index + 1
            }
        }

        if (decimalSeparatorIndex != -1) {
            transformedTextBuilder.append(decimalFormat.decimalFormatSymbols.decimalSeparator)
            originalToTransformedOffsets[decimalSeparatorIndex + 1] = transformedTextOffset
            transformedToOriginalOffsets[transformedTextOffset] = decimalSeparatorIndex + 1
            transformedTextOffset += 1
        }

        fractionPartIndices.forEach { index ->
            transformedTextBuilder.append(text.text[index])
            originalToTransformedOffsets[index + 1] = transformedTextOffset
            transformedToOriginalOffsets[transformedTextOffset] = index + 1
            transformedTextOffset += 1
        }

        transformedTextBuilder.append(suffix)

        repeat(suffix.length) {
            transformedToOriginalOffsets[transformedTextOffset] = text.text.lastIndex + 1
            transformedTextOffset += 1
        }

        val formattedText = transformedTextBuilder.toString()

        val scope = object : StyleNumberVisualTransformationScope {

            override val integerPartIndices: IntRange
                get() = integerPartIndices

            override val decimalSeparatorIndex: Int
                get() = decimalSeparatorIndex

            override val fractionPartIndices: IntRange
                get() = fractionPartIndices

            override val extraSymbolIndices: IntRange =
                when (
                    val startIndex =
                        formattedText.indexOf(decimalFormat.decimalFormatSymbols.currencySymbol)
                ) {
                    -1 -> IntRange.EMPTY
                    else -> startIndex.until(startIndex + decimalFormat.decimalFormatSymbols.currencySymbol.length)
                }
        }

        val newText = with(scope) { transformText(formattedText) }

        check(newText.length == formattedText.length) { "transformed text length should be equals to formatted text length" }

        return TransformedText(
            newText,
            NumberOffsetMapping(originalToTransformedOffsets, transformedToOriginalOffsets)
        )
    }

    private val IntRange.length: Int
        get() = when {
            isEmpty() -> 0
            step > 0 -> last - start + 1
            else -> start - last + 1
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StyleNumberVisualTransformation

        return decimalFormat == other.decimalFormat
    }

    override fun hashCode(): Int {
        return decimalFormat.hashCode()
    }

    override fun toString(): String {
        return "CurrencyStyleNumberVisualTransformation(decimalFormat=$decimalFormat)"
    }
}


internal data class DecimalFormat(
    val positiveSuffix: String,
    val negativeSuffix: String,
    val positivePrefix: String,
    val negativePrefix: String,
    val groupingSize: Int,
    val decimalFormatSymbols: DecimalFormatSymbols,
)

internal data class DecimalFormatSymbols(
    val groupingSeparator: Char,
    val decimalSeparator: Char,
    val currencySymbol: String,
)

internal fun getDefaultLocaleDecimalFormatByCurrencyCode(currencyCode: String): DecimalFormat {
    val locale = NSLocale(currencyCode)
    val numberFormatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
    }
    val currencySymbol = locale.displayNameForKey(NSLocaleCurrencySymbol, currencyCode)
        ?: numberFormatter.currencySymbol

    return DecimalFormat(
        positivePrefix = numberFormatter.positivePrefix,
        positiveSuffix = numberFormatter.positiveSuffix,
        negativePrefix = numberFormatter.negativePrefix,
        negativeSuffix = numberFormatter.negativeSuffix,
        groupingSize = numberFormatter.groupingSize.toInt(),
        decimalFormatSymbols = DecimalFormatSymbols(
            groupingSeparator = numberFormatter.groupingSeparator.first(),
            decimalSeparator = numberFormatter.decimalSeparator.first(),
            currencySymbol = currencySymbol
        )
    )
}
