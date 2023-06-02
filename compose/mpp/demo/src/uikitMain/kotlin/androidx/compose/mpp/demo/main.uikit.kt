// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import ApplicationLayoutExamples
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UINavigationController
import platform.UIKit.childViewControllers
import platform.UIKit.navigationController

@Composable
fun NativePopupPresenter() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val viewController = LocalUIViewController.current
        Button(onClick = {
            val navigationController = UINavigationController(rootViewController = ComposeUIViewController {
                NativePopupNavigationPage()
            })

            viewController.presentViewController(navigationController, true, null)
        }) {
            Text("Present popup")
        }
    }
}

@Composable
fun NativePopupNavigationPage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        val navigationController = LocalUIViewController.current.navigationController

        Button(onClick = {
            navigationController?.pushViewController(
                ComposeUIViewController {
                    NativePopupNavigationPage()
                }, true
            )
        }) {
            Text("Push")
        }
    }
}

fun main() {
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        IosDemo()
    })
}

@Composable
fun IosDemo() {
    // You may uncomment different examples:
    MultiplatformDemo()
//    ApplicationLayoutExamples()
}

@Composable
fun MultiplatformDemo() {
    val app = remember {
        App(extraScreens = listOf(Screen.Example("Native popup presenter") {
            NativePopupPresenter()
        }))
    }
    app.Content()
}
