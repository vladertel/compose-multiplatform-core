/*
 * Copyright 2022 The Android Open Source Project
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



import androidx.compose.foundation.layout.*
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import bouncingBalls.BouncingBallsApp
import org.jetbrains.skiko.wasm.onWasmReady


object JsTime : Time {
    override fun now(): Long = kotlinx.browser.window.performance.now().toLong()
}

fun main() {
    onWasmReady {
        Window("Falling Balls") {
            val selectedExample = remember { mutableStateOf(Examples.FallingBalls) }

            Column(modifier = Modifier.fillMaxSize()) {
                ExamplesChooser(selectedExample)
                Spacer(modifier = Modifier.height(24.dp))

                when (selectedExample.value) {
                    Examples.FallingBalls -> {
                        val game = remember { Game(JsTime) }
                        FallingBalls(game)
                    }
                    Examples.BouncingBalls -> {
                        BouncingBallsApp(10)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamplesChooser(selected: MutableState<Examples>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Choose an example: ", fontSize = 16.sp)

            Examples.values().forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected.value == it, onClick = {
                        selected.value = it
                    })
                    Text(it.name)
                }
            }
        }
    }
}

private enum class Examples {
    FallingBalls,
    BouncingBalls
}