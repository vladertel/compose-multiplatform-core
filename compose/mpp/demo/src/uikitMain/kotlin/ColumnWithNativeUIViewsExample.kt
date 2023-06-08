import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.ComposeUITextField
import androidx.compose.ui.unit.dp

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

val ColumnWithNativeUIViewsExample = Screen.Example("Column with native UIViews") {
    ColumnWithNativeUIViews()
}

@Composable
private fun ColumnWithNativeUIViews() {
    var value by remember { mutableStateOf("something") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(10) { _ ->
            Spacer(Modifier.fillMaxWidth().height(40.dp))
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text(value, Modifier.fillMaxWidth().height(40.dp))

                ComposeUITextField(value, {
                    value = it
                }, Modifier.fillMaxWidth().height(40.dp))
            }
        }

        items(40) { _ ->
            Spacer(Modifier.fillMaxWidth().height(40.dp))
        }
    }
}