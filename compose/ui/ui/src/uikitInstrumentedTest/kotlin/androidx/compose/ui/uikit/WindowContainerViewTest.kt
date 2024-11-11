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

package androidx.compose.ui.uikit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.test.dpRectInWindow
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectInset
import platform.UIKit.UIView
import platform.UIKit.UIViewController

class WindowContainerViewTest {
    @Test
    fun `test custom window container view`() = runUIKitInstrumentedTest {
        val padding = 40.dp
        val popupView = UIView()
        val uikitViewController = TestUIKitViewController(padding)
        @OptIn(ExperimentalComposeApi::class)
        val composeViewController = ComposeUIViewController(
            configure = {
                windowContainerView = uikitViewController.view
            }
        ) {
            Popup {
                UIKitView({ popupView }, modifier = Modifier.fillMaxSize())
            }
        } as ComposeHostingViewController
        uikitViewController.composeController = composeViewController

        setContent {
            UIKitViewController(
                factory = { uikitViewController },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        }
        waitUntil { !composeViewController.hasInvalidations() }

        val expectedComposeFrame = DpRect(
            left = padding * 2,
            top = padding * 2,
            right = screenSize.width - padding * 2,
            bottom = screenSize.height - padding * 2
        )
        val expectedPopupFrame = DpRect(
            left = padding,
            top = padding,
            right = screenSize.width - padding,
            bottom = screenSize.height - padding
        )

        assertEquals(
            expected = expectedComposeFrame,
            actual = composeViewController.view.dpRectInWindow()
        )
        assertEquals(
            expected = expectedPopupFrame,
            actual = popupView.dpRectInWindow()
        )
    }
}

private class TestUIKitViewController(
    private val padding: Dp
): UIViewController(nibName = null, bundle = null) {
    var composeController: UIViewController? = null
        set(value) {
            field = value
            value?.let {
                view.addSubview(it.view)
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        composeController?.view?.setFrame(
            CGRectInset(view.bounds, padding.value.toDouble(), padding.value.toDouble())
        )
    }
}
