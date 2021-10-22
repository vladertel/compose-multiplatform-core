/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoDispatchers
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.SkikoInputEvent
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerEvent
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal class ComposeLayer {
    private var isDisposed = false

    internal val layer = SkiaLayer()

    inner class ComponentImpl : SkikoView, PlatformComponent {
        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            val contentScale = layer.contentScale
            canvas.scale(contentScale, contentScale)
            scene.render(canvas/*, (width / contentScale).toInt(), (height / contentScale).toInt()*/, nanoTime)
            // Request next frame immediately.
            layer.needRedraw()
        }

        override fun onInputEvent(event: SkikoInputEvent) {
            TODO("need scene.sendInputEvent")
        }

        override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
            TODO("need scene.sendKeyEvent")
        }

        override fun onPointerEvent(event: SkikoPointerEvent) {
            TODO("need scene.sendPointerEvent")
        }
    }

    val view = ComponentImpl()

    init {
        layer.skikoView = view
    }

    private val scene = ComposeScene(
        SkikoDispatchers.Main,
        view,
        Density(1f),
        layer::needRedraw
    )

    fun dispose() {
        check(!isDisposed)
        scene.dispose()
        // events.cancel()

        // TODO: SkiaLayer.macos has disposeLayer, SkiaLayer.jvm  has dispose.
        // Should't we have a common dispose()?
        // layer.dispose()
        _initContent = null
        isDisposed = true
    }

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
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
