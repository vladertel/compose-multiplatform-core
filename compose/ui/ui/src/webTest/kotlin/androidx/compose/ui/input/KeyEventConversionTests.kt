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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.key.utf16CodePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.events.KeyboardEvent

private fun KeyboardEvent.assertEquivalence(key: Key, codePoint: Int = key.keyCode.toInt()) {
    val keyEvent = toComposeEvent()
    assertEquals(keyEvent.key, key)
    assertEquals(keyEvent.utf16CodePoint, codePoint)
}

class KeyEventConversionTests {

    @Test
    fun standardKeyboardConversionTest() {
        keyDownEvent("a", code = "KeyA").assertEquivalence(key = Key.A)
        keyDownEvent("b", code = "KeyB").assertEquivalence(key = Key.B)
        keyDownEvent("c", code = "KeyC").assertEquivalence(key = Key.C)
        keyDownEvent("d", code = "KeyD").assertEquivalence(key = Key.D)
        keyDownEvent("e", code = "KeyE").assertEquivalence(key = Key.E)
        keyDownEvent("f", code = "KeyF").assertEquivalence(key = Key.F)
        keyDownEvent("g", code = "KeyG").assertEquivalence(key = Key.G)
        keyDownEvent("h", code = "KeyH").assertEquivalence(key = Key.H)
        keyDownEvent("i", code = "KeyI").assertEquivalence(key = Key.I)
        keyDownEvent("j", code = "KeyJ").assertEquivalence(key = Key.J)
        keyDownEvent("k", code = "KeyK").assertEquivalence(key = Key.K)
        keyDownEvent("l", code = "KeyL").assertEquivalence(key = Key.L)
        keyDownEvent("m", code = "KeyM").assertEquivalence(key = Key.M)
        keyDownEvent("n", code = "KeyN").assertEquivalence(key = Key.N)
        keyDownEvent("o", code = "KeyO").assertEquivalence(key = Key.O)
        keyDownEvent("p", code = "KeyP").assertEquivalence(key = Key.P)
        keyDownEvent("q", code = "KeyQ").assertEquivalence(key = Key.Q)
        keyDownEvent("r", code = "KeyR").assertEquivalence(key = Key.R)
        keyDownEvent("s", code = "KeyS").assertEquivalence(key = Key.S)
        keyDownEvent("t", code = "KeyT").assertEquivalence(key = Key.T)
        keyDownEvent("u", code = "KeyU").assertEquivalence(key = Key.U)
        keyDownEvent("v", code = "KeyV").assertEquivalence(key = Key.V)
        keyDownEvent("w", code = "KeyW").assertEquivalence(key = Key.W)
        keyDownEvent("x", code = "KeyX").assertEquivalence(key = Key.X)
        keyDownEvent("y", code = "KeyY").assertEquivalence(key = Key.Y)
        keyDownEvent("z", code = "KeyZ").assertEquivalence(key = Key.Z)

//        keyDownEvent("0", code = "Key0").assertEquivalence(key = Key.Zero)
//        keyDownEvent("1", code = "Key1").assertEquivalence(key = Key.One)
//        keyDownEvent("2", code = "Key2").assertEquivalence(key = Key.Two)
//        keyDownEvent("3", code = "Key3").assertEquivalence(key = Key.Three)
//        keyDownEvent("4", code = "Key4").assertEquivalence(key = Key.Four)
//        keyDownEvent("5", code = "Key5").assertEquivalence(key = Key.Five)
//        keyDownEvent("6", code = "Key6").assertEquivalence(key = Key.Six)
//        keyDownEvent("7", code = "Key7").assertEquivalence(key = Key.Seven)
//        keyDownEvent("8", code = "Key8").assertEquivalence(key = Key.Eight)
//        keyDownEvent("9", code = "Key9").assertEquivalence(key = Key.Nine)
    }

}