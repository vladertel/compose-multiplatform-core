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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import platform.CoreGraphics.CGRectInset
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewController

val CustomWindowContainerExample = Screen.Example("Custom windowContainerView example") {
    Box(modifier = Modifier.displayCutoutPadding()) {
        UIKitViewController(
            factory = ::EmbeddedViewController,
            modifier = Modifier.padding(20.dp).fillMaxSize()
        )
        Text(
            text = "Alert should be inside the blue area",
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private class EmbeddedViewController: UIViewController(nibName = null, bundle = null) {
    private val composeController by lazy { EmbeddedComposeViewController(view) }

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.layer.borderWidth = 1.0
        view.layer.borderColor = UIColor.blueColor.CGColor

        view.addSubview(composeController.view)
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        composeController.view.setFrame(CGRectInset(view.bounds, 30.0, 30.0))
    }
}

@OptIn(ExperimentalComposeApi::class)
private fun EmbeddedComposeViewController(view: UIView) = ComposeUIViewController(
    configure = {
        this.windowContainerView = view
    }
) {
    MaterialTheme {
        val text = rememberTextFieldState()
        val showAlert = remember { mutableStateOf(false) }

        Column(modifier = Modifier.border(2.dp, Color.Green.copy(alpha = .3f)).fillMaxSize()) {
            TextField(text, modifier = Modifier.fillMaxWidth())
            Button({ showAlert.value = true }) {
                Text("Show Alert")
            }

            if (showAlert.value) {
                AlertDialog(
                    onDismissRequest = { showAlert.value = false },
                    confirmButton = {
                        TextButton({ showAlert.value = false }) {
                            Text("Submit")
                        }
                    },
                    title = {
                        Text("Hello Alert")
                    },
                    text = {
                        Text("Alert message")
                    }
                )
            }
        }
    }
}