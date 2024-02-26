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

import androidx.compose.material3.Slider
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow

class Store(
    val value: MutableStateFlow<Float> = MutableStateFlow(0.5f)
) {
    fun progress(it: Float) {
        value.tryEmit(it)
        println(it)
    }
    fun finished() = println("finished")
}

// https://github.com/JetBrains/compose-multiplatform/issues/4366
val Material3SliderBug = Screen.Example(
    "Material3 Slider bug #4366"
) {
    val store = remember {
        Store()
    }
    val value by store.value.collectAsState()
    Slider(
        value = value,
        onValueChange = { store.progress(it) },
        onValueChangeFinished = { store.finished() }
    )
}