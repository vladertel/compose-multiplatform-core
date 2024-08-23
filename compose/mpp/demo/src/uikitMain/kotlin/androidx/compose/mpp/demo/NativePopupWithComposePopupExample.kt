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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UISheetPresentationControllerDetentIdentifierLarge
import platform.UIKit.UISheetPresentationControllerDetentIdentifierMedium
import platform.UIKit.sheetPresentationController

val NativePopupWithComposePopupExample = Screen.Example("Native popup with Compose popup") {
    val viewController = LocalUIViewController.current

    Button(onClick = {
        val presentedViewController = ComposeUIViewController {
            var showComposePopup by remember { mutableStateOf(false) }
            var showComposeDialog by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    showComposePopup = true
                }) {
                    Text("Show Compose popup")
                }

                Button(onClick = {
                    showComposeDialog = true
                }) {
                    Text("Show Compose dialog")
                }
            }

            if (showComposePopup) {
                Popup(
                    onDismissRequest = {
                        showComposePopup = false
                    },
                    properties = PopupProperties(
                        dismissOnClickOutside = true
                    )
                ) {
                    Text("Compose popup", modifier = Modifier.background(Color.LightGray))
                }
            }

            if (showComposeDialog) {
                Dialog(
                    onDismissRequest = {
                        showComposeDialog = false
                    },
                ) {
                    Text("Compose dialog", modifier = Modifier.background(Color.Green))
                }
            }
        }

        presentedViewController.modalPresentationStyle = UIModalPresentationFormSheet
        presentedViewController.sheetPresentationController?.apply {
            detents = listOf(
                UISheetPresentationControllerDetent.largeDetent(),
                UISheetPresentationControllerDetent.mediumDetent()
            )

            prefersGrabberVisible = true
        }
        viewController.presentViewController(presentedViewController, true, null)
    }) {
        Text("Show native popup")
    }
}