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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color

@Composable
actual fun ScaffoldInteropBugTopBar(modifier: Modifier) {
    SwingPanel(Color.Red, factory = {
        javax.swing.JPanel().also {
            it.background = java.awt.Color.RED
        }
    }, modifier = modifier)
}

@Composable
actual fun ScaffoldInteropBugContent(modifier: Modifier) {
    SwingPanel(Color.Blue, factory = {
        javax.swing.JPanel().also {
            it.background = java.awt.Color.BLUE
        }
    }, modifier = modifier)
}