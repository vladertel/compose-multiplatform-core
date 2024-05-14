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
    assertEquals(actual = keyEvent.key, expected = key)
    assertEquals(actual = keyEvent.utf16CodePoint, expected = codePoint)
}

class KeyEventConversionTests {

    @Test
    fun standardKeyboardConversionTest() {
        keyDownEvent("a", code = "KeyA").assertEquivalence(key = Key.A, codePoint = 97)
        keyDownEvent("b", code = "KeyB").assertEquivalence(key = Key.B, codePoint = 98)
        keyDownEvent("c", code = "KeyC").assertEquivalence(key = Key.C, codePoint = 99)
        keyDownEvent("d", code = "KeyD").assertEquivalence(key = Key.D, codePoint = 100)
        keyDownEvent("e", code = "KeyE").assertEquivalence(key = Key.E, codePoint = 101)
        keyDownEvent("f", code = "KeyF").assertEquivalence(key = Key.F, codePoint = 102)
        keyDownEvent("g", code = "KeyG").assertEquivalence(key = Key.G, codePoint = 103)
        keyDownEvent("h", code = "KeyH").assertEquivalence(key = Key.H, codePoint = 104)
        keyDownEvent("i", code = "KeyI").assertEquivalence(key = Key.I, codePoint = 105)
        keyDownEvent("j", code = "KeyJ").assertEquivalence(key = Key.J, codePoint = 106)
        keyDownEvent("k", code = "KeyK").assertEquivalence(key = Key.K, codePoint = 107)
        keyDownEvent("l", code = "KeyL").assertEquivalence(key = Key.L, codePoint = 108)
        keyDownEvent("m", code = "KeyM").assertEquivalence(key = Key.M, codePoint = 109)
        keyDownEvent("n", code = "KeyN").assertEquivalence(key = Key.N, codePoint = 110)
        keyDownEvent("o", code = "KeyO").assertEquivalence(key = Key.O, codePoint = 111)
        keyDownEvent("p", code = "KeyP").assertEquivalence(key = Key.P, codePoint = 112)
        keyDownEvent("q", code = "KeyQ").assertEquivalence(key = Key.Q, codePoint = 113)
        keyDownEvent("r", code = "KeyR").assertEquivalence(key = Key.R, codePoint = 114)
        keyDownEvent("s", code = "KeyS").assertEquivalence(key = Key.S, codePoint = 115)
        keyDownEvent("t", code = "KeyT").assertEquivalence(key = Key.T, codePoint = 116)
        keyDownEvent("u", code = "KeyU").assertEquivalence(key = Key.U, codePoint = 117)
        keyDownEvent("v", code = "KeyV").assertEquivalence(key = Key.V, codePoint = 118)
        keyDownEvent("w", code = "KeyW").assertEquivalence(key = Key.W, codePoint = 119)
        keyDownEvent("x", code = "KeyX").assertEquivalence(key = Key.X, codePoint = 120)
        keyDownEvent("y", code = "KeyY").assertEquivalence(key = Key.Y, codePoint = 121)
        keyDownEvent("z", code = "KeyZ").assertEquivalence(key = Key.Z, codePoint = 122)

        keyDownEvent("0", code = "Digit0").assertEquivalence(key = Key.Zero)
        keyDownEvent("1", code = "Digit1").assertEquivalence(key = Key.One)
        keyDownEvent("2", code = "Digit2").assertEquivalence(key = Key.Two)
        keyDownEvent("3", code = "Digit3").assertEquivalence(key = Key.Three)
        keyDownEvent("4", code = "Digit4").assertEquivalence(key = Key.Four)
        keyDownEvent("5", code = "Digit5").assertEquivalence(key = Key.Five)
        keyDownEvent("6", code = "Digit6").assertEquivalence(key = Key.Six)
        keyDownEvent("7", code = "Digit7").assertEquivalence(key = Key.Seven)
        keyDownEvent("8", code = "Digit8").assertEquivalence(key = Key.Eight)
        keyDownEvent("9", code = "Digit9").assertEquivalence(key = Key.Nine)

        keyDownEvent("0", code = "Digit0").assertEquivalence(key = Key.Zero)
        keyDownEvent("1", code = "Digit1").assertEquivalence(key = Key.One)
        keyDownEvent("2", code = "Digit2").assertEquivalence(key = Key.Two)
        keyDownEvent("3", code = "Digit3").assertEquivalence(key = Key.Three)
        keyDownEvent("4", code = "Digit4").assertEquivalence(key = Key.Four)
        keyDownEvent("5", code = "Digit5").assertEquivalence(key = Key.Five)
        keyDownEvent("6", code = "Digit6").assertEquivalence(key = Key.Six)
        keyDownEvent("7", code = "Digit7").assertEquivalence(key = Key.Seven)
        keyDownEvent("8", code = "Digit8").assertEquivalence(key = Key.Eight)
        keyDownEvent("9", code = "Digit9").assertEquivalence(key = Key.Nine)

        keyDownEvent("0", code = "Numpad0").assertEquivalence(key = Key.NumPad0, codePoint = 48)
        keyDownEvent("1", code = "Numpad1").assertEquivalence(key = Key.NumPad1, codePoint = 49)
        keyDownEvent("2", code = "Numpad2").assertEquivalence(key = Key.NumPad2, codePoint = 50)
        keyDownEvent("3", code = "Numpad3").assertEquivalence(key = Key.NumPad3, codePoint = 51)
        keyDownEvent("4", code = "Numpad4").assertEquivalence(key = Key.NumPad4, codePoint = 52)
        keyDownEvent("5", code = "Numpad5").assertEquivalence(key = Key.NumPad5, codePoint = 53)
        keyDownEvent("6", code = "Numpad6").assertEquivalence(key = Key.NumPad6, codePoint = 54)
        keyDownEvent("7", code = "Numpad7").assertEquivalence(key = Key.NumPad7, codePoint = 55)
        keyDownEvent("8", code = "Numpad8").assertEquivalence(key = Key.NumPad8, codePoint = 56)
        keyDownEvent("9", code = "Numpad9").assertEquivalence(key = Key.NumPad9, codePoint = 57)

        keyDownEvent("Backspace", code = "Backspace").assertEquivalence(key = Key.Backspace)
        keyDownEvent("Delete", code = "Delete").assertEquivalence(key = Key.Delete)

        keyDownEvent("Control", code = "ControlLeft").assertEquivalence(key = Key.CtrlLeft)
        keyDownEvent("Control", code = "ControlRight").assertEquivalence(key = Key.CtrlRight)
        keyDownEvent("Meta", code = "MetaLeft").assertEquivalence(key = Key.MetaLeft)
        keyDownEvent("Meta", code = "MetaRight").assertEquivalence(key = Key.MetaRight)
        keyDownEvent("Alt", code = "AltLeft").assertEquivalence(key = Key.AltLeft)
        keyDownEvent("Alt", code = "AltRight").assertEquivalence(key = Key.AltRight)
    }

}
