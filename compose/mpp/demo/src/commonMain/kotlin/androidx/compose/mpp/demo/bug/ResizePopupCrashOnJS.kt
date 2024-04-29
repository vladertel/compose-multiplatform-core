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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.border
import androidx.compose.mpp.demo.Screen
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Color

@OptIn(ExperimentalMaterialApi::class)
internal val ResizePopupCrashOnJS = Screen.Example("Screen Orientation") {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(
            modifier = Modifier.weight(0.5f)
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.primaryVariant
                )
        )
        Box(
            modifier = Modifier.weight(0.5f)
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.primaryVariant
                )
        )
    }}
