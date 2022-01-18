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

package androidx.compose.ui.awt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.event.MouseEvent.BUTTON1_DOWN_MASK
import java.awt.event.MouseEvent.MOUSE_ENTERED
import java.awt.event.MouseEvent.MOUSE_MOVED
import java.awt.event.MouseEvent.MOUSE_PRESSED
import java.awt.event.MouseEvent.MOUSE_RELEASED
import org.junit.Test

class ComposeWindowTest {
    // bug https://github.com/JetBrains/compose-jb/issues/1448
    @Test
    fun `dispose window in event handler`() = runApplicationTest {
        val window = ComposeWindow()
        try {
            var isClickHappened = false
            window.size = Dimension(300, 400)
            window.setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                    isClickHappened = true
                    window.dispose()
                })
            }
            window.isVisible = true
            window.sendMouseEvent(MOUSE_ENTERED, 100, 50)
            awaitIdle()
            window.sendMouseEvent(MOUSE_MOVED, 100, 50)
            awaitIdle()
            window.sendMouseEvent(MOUSE_PRESSED, 100, 50, modifiers = BUTTON1_DOWN_MASK)
            awaitIdle()
            window.sendMouseEvent(MOUSE_RELEASED, 100, 50)
            awaitIdle()
            assertThat(isClickHappened).isTrue()
        } finally {
            window.dispose()
        }
    }
}