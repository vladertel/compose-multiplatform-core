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

<<<<<<<< HEAD:compose/mpp/demo/src/commonMain/kotlin/androidx/compose/mpp/demo/textfield/android/TextFieldsInDialogDemo.kt
package androidx.compose.mpp.demo.textfield.android

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun TextFieldsInDialogDemo() {
    Text("Compose Multiplatform currently not have Dialog() like on Android")
}
========
package androidx.compose.material3.adaptive.navigation.suite

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal actual val WindowAdaptiveInfoDefault: WindowAdaptiveInfo
    @Composable
    get() = currentWindowAdaptiveInfo()
>>>>>>>> jetpack-compose/1.6.0-beta03:compose/material3/material3-adaptive-navigation-suite/src/androidMain/kotlin/androidx/compose/material3/adaptive/navigation-suite/NavigationSuiteScaffold.android.kt
