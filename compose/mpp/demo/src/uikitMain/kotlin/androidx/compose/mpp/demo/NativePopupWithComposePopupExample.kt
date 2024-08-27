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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.sheetPresentationController

@OptIn(ExperimentalComposeUiApi::class)
val NativePopupWithComposePopupExample = Screen.Example("Native popup with Compose popup") {
    val viewController = LocalUIViewController.current

    Button(onClick = {
        val presentedViewController = UIViewController(nibName = null, bundle = null)

        val composeViewController = ComposeUIViewController {
            var showComposePopup by remember { mutableStateOf(false) }
            var showComposeDialog by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    showComposePopup = true
                }) {
                    Text("Show Compose popup")

                    if (showComposePopup) {
                        Popup(
                            onDismissRequest = {
                                showComposePopup = false
                            },
                            properties = PopupProperties(
                                usePlatformDefaultWidth = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Text(
                                text ="Compose popup",
                                modifier = Modifier.background(Color.LightGray)
                            )
                        }
                    }
                }

                Button(onClick = {
                    showComposeDialog = true
                }) {
                    Text("Show Compose dialog")

                    if (showComposeDialog) {
                        Dialog(
                            onDismissRequest = {
                                showComposeDialog = false
                            },
                            properties = DialogProperties(
                                dismissOnClickOutside = true,
                            )
                        ) {
                            Text(
                                text = "Compose dialog",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Green)
                            )
                        }
                    }
                }
            }
        }

        presentedViewController.addChildViewController(composeViewController)
        presentedViewController.view.addSubview(composeViewController.view)
        composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
        composeViewController.view.layer.borderColor = UIColor.redColor.CGColor
        composeViewController.view.layer.borderWidth = 2.0
        NSLayoutConstraint.activateConstraints(
            listOf(
                composeViewController.view.centerXAnchor.constraintEqualToAnchor(presentedViewController.view.centerXAnchor),
                composeViewController.view.centerYAnchor.constraintEqualToAnchor(presentedViewController.view.centerYAnchor),
                composeViewController.view.widthAnchor.constraintEqualToAnchor(presentedViewController.view.widthAnchor, 0.5),
                composeViewController.view.heightAnchor.constraintEqualToAnchor(presentedViewController.view.heightAnchor, 0.5)
            )
        )
        composeViewController.didMoveToParentViewController(presentedViewController)

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