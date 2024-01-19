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

import androidx.compose.foundation.text.KeyMapping
import androidx.compose.foundation.text.createMacosDefaultKeyMapping
import androidx.compose.foundation.text.defaultSkikoKeyMapping
import androidx.compose.foundation.text.overriddenDefaultKeyMapping
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DesktopSelectionTests(keyboardActions: KeyboardActions, keyMapping: KeyMapping) :
    CommonSelectionTests(
        keyboardActions, keyMapping
    ) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "with {0}")
        fun initParameters() = arrayOf(
            arrayOf(DefaultKeyboardActions, defaultSkikoKeyMapping),
            arrayOf(MacosKeyboardActions, createMacosDefaultKeyMapping())
        )
    }

    override fun setPlatformDefaultKeyMapping(value: KeyMapping) {
        overriddenDefaultKeyMapping = value
    }

    @Test
    override fun selectLineStartTest() {
        selectLineStart()
    }

    @Test
    override fun selectTextStartTest() {
        selectTextStart()
    }

    @Test
    override fun selectTextEndTest() {
        selectTextEnd()
    }

    @Test
    override fun selectLineEndTest() {
        selectLineEnd()
    }

    @Test
    override fun deleteAllTest() {
        deleteAll()
    }

    @Test
    override fun selectAllTest() {
        selectAll()
    }
}
