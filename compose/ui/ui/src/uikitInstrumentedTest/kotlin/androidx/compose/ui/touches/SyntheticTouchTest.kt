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

package androidx.compose.ui.touches

import kotlin.test.Test

class KeyboardInsetsTest {

    @Test
    fun runAppTest() {
//        var controller: UIViewController? = null
//        defaultUIKitMain("TestApplication") {
//            controller = UIViewController()
//            controller!!
//        }

        try {
            error("Stack")
        } catch (e: Throwable) {
            e.printStackTrace()
        }

//        assertNotNull(controller)
    }

//    @Test
//    fun `test button tap`() = runUIKitInstrumentedTest {
//        var clicked = false
//        setContent {
//            Button(
//                onClick = {
//                    println(">> Clicked!!!!!")
//                    println(">> Clicked!!!!!")
//                    clicked = true
//                },
//                modifier = Modifier.fillMaxSize()
//            ) {
//                Box {}
//            }
//        }
//        //tap(screenSize.center)
//
//        assertTrue(clicked)
//    }
}

//internal suspend fun UIKitInstrumentedTest.tap(point: DpOffset) {
//    val touch = SyntheticTouch(view, point)
//
//    println(">>> View - ${touch.view}")
//    touch.setPhaseAndUpdateTimestamp(UITouchPhase.UITouchPhaseBegan)
//
//    touch.view?.touchesBegan(setOf(touch), withEvent = null)
//
//    delay(150)
//
//    touch.setPhaseAndUpdateTimestamp(UITouchPhase.UITouchPhaseEnded)
//
//    touch.view?.touchesEnded(setOf(touch), withEvent = null)
//
//    if (touch.view?.canBecomeFirstResponder == true) {
//        touch.view?.becomeFirstResponder()
//    }
//}