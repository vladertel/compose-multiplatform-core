import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.NSLayoutConstraintMeta
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.navigationController

/*
 * Copyright 2023 The Android Open Source Project
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

val NativeModalWithNaviationExample = Screen.Example("Native modal with navigation") {
    NativeModalWithNavigation()
}
@Composable
private fun NativeModalWithNavigation() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val viewController = LocalUIViewController.current
        Button(onClick = {
            val navigationController = UINavigationController(rootViewController = ComposeUIViewController {
                NativeNavigationPage()
            })

            viewController.presentViewController(navigationController, true, null)
        }) {
            Text("Present popup")
        }
    }
}

@Composable
private fun NativeNavigationPage() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            val navigationController = LocalUIViewController.current.navigationController

            val colors = listOf(
                "White" to Color.White,
                "Green" to Color.Green,
                "Black" to Color.Black,
                "Semitransparent Black" to Color(0x88000000),
                "Transparent" to Color(0x00000000),
                "Semitransparent Red" to Color(0x88FF0000),
            )

            for (color in colors) {
                Button(onClick = {
                    val viewController = UIViewController()
                    viewController.view.backgroundColor = UIColor.whiteColor

                    val label = UILabel()
                    label.textColor = UIColor.blackColor
                    label.numberOfLines = 0
                    label.text =
                        """
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque scelerisque fermentum sem, at vestibulum quam. Donec euismod turpis non augue vestibulum, id varius metus tincidunt
                        """.trimIndent()
                    label.translatesAutoresizingMaskIntoConstraints = false
                    viewController.view.addSubview(label)

                    val composeViewController = ComposeUIViewController(configure = {
                        backgroundColor = color.second
                    }) {
                        NativeNavigationPage()
                    }

                    viewController.addChildViewController(composeViewController)
                    composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
                    viewController.view.addSubview(composeViewController.view)
                    composeViewController.didMoveToParentViewController(viewController)

                    NSLayoutConstraint.activateConstraints(listOf(
                        label.topAnchor.constraintEqualToAnchor(viewController.view.topAnchor),
                        label.bottomAnchor.constraintEqualToAnchor(viewController.view.bottomAnchor),
                        label.leftAnchor.constraintEqualToAnchor(viewController.view.leftAnchor),
                        label.rightAnchor.constraintEqualToAnchor(viewController.view.rightAnchor),

                        composeViewController.view.topAnchor.constraintEqualToAnchor(viewController.view.topAnchor),
                        composeViewController.view.bottomAnchor.constraintEqualToAnchor(viewController.view.bottomAnchor),
                        composeViewController.view.leftAnchor.constraintEqualToAnchor(viewController.view.leftAnchor),
                        composeViewController.view.rightAnchor.constraintEqualToAnchor(viewController.view.rightAnchor)
                    ))

                    navigationController?.pushViewController(
                        viewController,
                        animated = true
                    )
                }) {
                    Text(color.first)
                }
            }
        }
    }
}
