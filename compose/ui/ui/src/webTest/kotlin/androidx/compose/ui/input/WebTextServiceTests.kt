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

package androidx.compose.ui.input

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.window.ComposeViewport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.events.UIEvent


class WebTextServiceTests {
    private lateinit var canvasContainer: Element

    @BeforeTest
    fun setUp() {
        canvasContainer = document.createElement("div")
        document.body!!.appendChild(canvasContainer)
    }

    @AfterTest
    fun tearDown() {
        canvasContainer.remove()
    }

    @Test
    fun noVirtualKeyboardByDefault()  {
        val textValue = mutableStateOf("")
        val fr = FocusRequester()

        val keyEventsLog = mutableListOf<KeyEvent>()

        ComposeViewport(canvasContainer) {
            TextField(
                value = textValue.value,
                onValueChange = {
                    textValue.value = it
                },
                modifier = Modifier.fillMaxSize().focusRequester(fr).onKeyEvent {
                    keyEventsLog.add(it)
                    true
                }
            )
            SideEffect {
                fr.requestFocus()
            }
        }

        val canvas = document.querySelector("canvas")!!

        assertEquals(0, document.querySelectorAll("textarea").length)


        canvas.dispatchEvent(keyDownEvent('t'))
        canvas.dispatchEvent(keyDownEvent('y'))
        canvas.dispatchEvent(keyDownEvent('p'))
        canvas.dispatchEvent(keyDownEvent('e'))
        canvas.dispatchEvent(keyDownEvent('d'))

        assertEquals(5, keyEventsLog.size)

        assertEquals(textValue.value, "typed")
    }

//    @Test
//    fun virtualKeyboardOnTouchEvent()  {
//        val textValue = mutableStateOf("")
//        val fr = FocusRequester()
//
//        ComposeViewport(canvasContainer) {
//            TextField(
//                value = textValue.value,
//                onValueChange = {
//                    textValue.value = it
//                },
//                modifier = Modifier.fillMaxSize().focusRequester(fr)
//            )
//            SideEffect {
//                fr.requestFocus()
//            }
//        }
//
//        val canvas = document.querySelector("canvas")!!
//
//        try {
//            canvas.dispatchEvent(TouchEvent("touchstart"))
//        } catch (e: Exception) {
//            println("=========== \n")
//            println(e)
//            println("=========== \n")
//        }
//
//        assertEquals(0, document.querySelectorAll("textarea").length)
//
//        canvas.dispatchEvent(keyDownEvent('t'))
//        canvas.dispatchEvent(keyDownEvent('y'))
//        canvas.dispatchEvent(keyDownEvent('p'))
//        canvas.dispatchEvent(keyDownEvent('e'))
//        canvas.dispatchEvent(keyDownEvent('d'))
//
//
//        assertEquals(textValue.value, "typed")
//    }

}


// TODO: we can not use definition for stdlib due to inconsistencies in declaration
private external class TouchEvent(type: String) : UIEvent
