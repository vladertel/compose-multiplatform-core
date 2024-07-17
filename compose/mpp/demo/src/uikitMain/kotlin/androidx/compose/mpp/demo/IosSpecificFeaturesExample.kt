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

package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.cupertino.DragSource
import androidx.compose.ui.draganddrop.cupertino.DropTarget
import androidx.compose.ui.draganddrop.cupertino.dragAndDrop
import androidx.compose.ui.draganddrop.cupertino.toUIDragItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol

val HapticFeedbackExample = Screen.Example("Haptic feedback") {
    val feedback = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }) {
            Text("TextHandleMove")
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }) {
            Text("LongPress")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
val DragAndDropExample = Screen.Example("Drag and drop") {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        var text by remember { mutableStateOf("Hello world!") }

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Payload content") },
        )

        Text(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(1f)
                .height(100.dp)
                .background(Color.DarkGray)
                .dragAndDrop(
                    dragSource = object : DragSource {
                        override fun UIDragInteraction.itemsForBeginningSession(session: UIDragSessionProtocol): List<UIDragItem> {
                            return listOf(
                                text.toUIDragItem()
                            )
                        }
                    }
                )
            ,
            color = Color.White,
            text = text
        )

        Spacer(modifier = Modifier.height(20.dp))

        var dropText by remember { mutableStateOf("Drop here") }

        Text(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(1f)
                .height(100.dp)
                .background(Color.DarkGray)
                .dragAndDrop(
                    dropTarget = object : DropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            dropText = event.items.firstOrNull()?.stringRepresentation ?: "No data"
                            return true
                        }
                    }
                )
            ,
            color = Color.White,
            text = dropText
        )
    }
}

val IosSpecificFeatures = Screen.Selection(
    "iOS-specific features",
    NativeModalWithNaviationExample,
    HapticFeedbackExample,
    LazyColumnWithInteropViewsExample,
    AccessibilityLiveRegionExample,
    InteropViewAndSemanticsConfigMerge,
    StatusBarStateExample,
    DragAndDropExample
)