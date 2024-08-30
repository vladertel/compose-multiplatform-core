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

package androidx.tv.material3.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
<<<<<<<< HEAD:tv/tv-material/samples/src/main/java/androidx/tv/material3/samples/SwitchSamples.kt
import androidx.tv.material3.Switch

@Sampled
@Composable
fun SwitchSample() {
    Switch(checked = true, onCheckedChange = {})
========
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowSizeClass

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val size = with(density) { windowInfo.containerSize.toSize().toDpSize() }
    return WindowAdaptiveInfo(
        WindowSizeClass.compute(size.width.value, size.height.value),
        Posture() //postures and hinges are relevant to android devices only
    )
>>>>>>>> 16b3858d2015621b889483c361d1c3926947b81f:compose/material3/adaptive/adaptive/src/skikoMain/kotlin/androidx/compose/material3/adaptive/WindowAdaptiveInfo.skiko.kt
}
