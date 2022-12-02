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

package androidx.compose.material

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.internal.keyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SliderTest {

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun slider_0_steps__ltr__changes_values_when_arrows_pressed() = runSkikoComposeUiTest {
        val state = mutableStateOf(0.5f)
        var sliderFocused = false
        setContent {
            Slider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..1f,
                modifier = Modifier.onFocusChanged {
                    sliderFocused = it.isFocused
                }
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyUp))

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.50f + (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.53f - (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.PageDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.PageDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.50f + (1 + it) / 10f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.PageUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.PageUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.80f - (1 + it) / 10f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.50f + (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.53f - (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        onRoot().performKeyPress(keyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle {
            assertEquals(1f, state.value)
        }

        onRoot().performKeyPress(keyEvent(Key.Home, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.Home, KeyEventType.KeyUp))
        runOnIdle {
            assertEquals(0f, state.value)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `slider_0_steps__rtl__changes_values_when_arrows_pressed`() = runSkikoComposeUiTest {
        val state = mutableStateOf(0.5f)
        var sliderFocused = false
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Slider(
                    value = state.value,
                    onValueChange = { state.value = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.onFocusChanged {
                        sliderFocused = it.isFocused
                    }
                )
            }
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle {
            assertTrue(sliderFocused)
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.50f - (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((0.47f + (1 + it) / 100f).round2decPlaces(), (state.value).round2decPlaces())
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `slider_29_steps__ltr__changes_values_when_arrows_pressed`() = runSkikoComposeUiTest {
        val state = mutableStateOf(15f)
        var sliderFocused = false
        setContent {
            Slider(
                value = state.value,
                steps = 29,
                onValueChange = { state.value = it },
                valueRange = 0f..30f,
                modifier = Modifier.onFocusChanged {
                    sliderFocused = it.isFocused
                }
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle {
            assertTrue(sliderFocused)
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((15f + (1f + it)), (state.value))
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((18f - (1 + it)), state.value)
            }
        }

        runOnIdle {
            state.value = 0f
        }

        val page = ((29 + 1) / 10).coerceIn(1, 10) // same logic as in Slider slideOnKeyEvents

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.PageDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.PageDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals((1f + it) * page, state.value)
            }
        }

        runOnIdle {
            state.value = 30f
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.PageUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.PageUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(30f - (1 + it) * page, state.value)
            }
        }

        runOnIdle {
            state.value = 0f
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(1f + it, state.value)
            }
        }

        repeat(3) {
            onRoot().performKeyPress(keyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(keyEvent(Key.DirectionDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(3f - (1f + it), state.value)
            }
        }

        onRoot().performKeyPress(keyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle {
            assertEquals(30f, state.value)
        }

        onRoot().performKeyPress(keyEvent(Key.Home, KeyEventType.KeyDown))
        onRoot().performKeyPress(keyEvent(Key.Home, KeyEventType.KeyUp))
        runOnIdle {
            assertEquals(0f, state.value)
        }
    }

    @Test
    fun `Slider_should_request_focus_on_Tap`() = runSkikoComposeUiTest {
        var hasFocus = false
        setContent {
            Slider(
                value = 0.1f,
                onValueChange = {},
                modifier = Modifier.onFocusChanged {
                    hasFocus = it.isFocused
                }.testTag("slider")
            )
        }

        onNodeWithTag("slider").performTouchInput {
            down(Offset(10f, 5f))
            up()
        }

        runOnIdle {
            assertEquals(true, hasFocus)
        }
    }
}

private fun Float.round2decPlaces() = (this * 100).roundToInt() / 100f
