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

package androidx.compose.ui.scene

import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.MetalView
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.UIKit.UIView

/**
 * A mediator for the primary [ComposeScene], that owns its own rendering view.
 */
internal class PrimaryComposeSceneMediator(
    parentView: UIView,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack?,
    windowContext: PlatformWindowContext,
    measureDrawLayerBounds: Boolean = false,
    coroutineContext: CoroutineContext,
    metalViewFactory: (UIKitInteropContainer, SkikoRenderDelegate) -> MetalView,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene
): ComposeSceneMediator(
    parentView,
    configuration,
    focusStack,
    windowContext,
    measureDrawLayerBounds,
    coroutineContext,
    metalViewFactory,
    composeSceneFactory
) {
}