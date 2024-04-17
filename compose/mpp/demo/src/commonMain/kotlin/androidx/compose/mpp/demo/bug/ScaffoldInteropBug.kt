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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.graphics.Color

@Composable
expect fun ScaffoldInteropBugTopBar(modifier: Modifier)

@Composable
expect fun ScaffoldInteropBugContent(modifier: Modifier)


val ScaffoldInteropBug = Screen.Fullscreen("Scaffol interop bug") {
    Row(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(),
            topBar = {
                ScaffoldInteropBugTopBar(Modifier.size(40.dp))
            },
            content = {
                ScaffoldInteropBugContent(Modifier.fillMaxSize())
            }
        )

        Divider(Modifier.fillMaxHeight().width(1.dp).background(Color.Black))

        androidx.compose.material.Scaffold(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            topBar = {
                ScaffoldInteropBugTopBar(Modifier.size(40.dp))
            },
            content = {
                ScaffoldInteropBugContent(Modifier.fillMaxSize())
            }
        )
    }
}