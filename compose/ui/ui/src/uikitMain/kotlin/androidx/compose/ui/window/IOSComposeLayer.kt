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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.toCompose
import androidx.compose.ui.native.getMainDispatcher
import androidx.compose.ui.native.getScrollDelta
import androidx.compose.ui.native.supportsMultitouch
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.Platform
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toDpRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.currentNanoTime

// TODO: refactor candidate on iOS, proxies everything and doesn't contain any logic
internal class IOSComposeLayer(
    platform: Platform,
    // Should be set to an actual value by ComposeWindow implementation
    var density: Density,
    needRedraw: () -> Unit
) {
    private var isDisposed = false

    val scene = ComposeScene(
        coroutineContext = getMainDispatcher(),
        platform = platform,
        density = density,
        invalidate = needRedraw,
    )

    fun dispose() {
        check(!isDisposed)
        scene.close()
        _initContent = null
        isDisposed = true
    }

    fun getActiveFocusRect(): DpRect? {
        val focusRect = scene.mainOwner?.focusOwner?.getFocusRect() ?: return null
        return focusRect.toDpRect(density)
    }

    fun hitInteropView(point: Point, isTouchEvent: Boolean): Boolean =
        scene.mainOwner?.hitInteropView(
            pointerPosition = Offset(point.x * density.density, point.y * density.density),
            isTouchEvent = isTouchEvent,
        ) ?: false

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }

        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        // TODO: do we need isDisplayable on SkiaLyer?
        // if (layer.isDisplayable) {
        _initContent?.invoke()
        _initContent = null
        // }
    }
}

private fun currentMillis() = (currentNanoTime() / 1E6).toLong()