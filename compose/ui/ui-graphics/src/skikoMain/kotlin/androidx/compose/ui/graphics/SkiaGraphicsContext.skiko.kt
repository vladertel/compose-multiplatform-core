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

package androidx.compose.ui.graphics

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.LayerManager
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Picture

@InternalComposeUiApi
class SkiaGraphicsContext() : GraphicsContext {
    private val layerManager = LayerManager()
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    init {
        snapshotObserver.start()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
    }

    fun drawIntoCanvas(canvas: Canvas, block: (Canvas) -> Unit) {
        val wrapperCanvas = object : org.jetbrains.skia.PictureFilterCanvas(canvas.nativeCanvas) {
            override fun onDrawPicture(
                picture: Picture,
                matrix: Matrix33?,
                paint: Paint?
            ): Boolean = layerManager.drawPlaceholder(picture, this) // Shouldn't this be [canvas.nativeCanvas]?
        }
        block(wrapperCanvas.asComposeCanvas())
    }

    override fun createGraphicsLayer(): GraphicsLayer {
        return GraphicsLayer(snapshotObserver, layerManager)
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        layer.release()
    }
}
