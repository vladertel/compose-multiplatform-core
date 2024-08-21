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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSTimeInterval
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta

internal class MetalView(
    private val renderDelegate: SkikoRenderDelegate,
    private val retrieveInteropTransaction: () -> UIKitInteropTransaction,
) : UIView(frame = CGRectZero.readValue()) {
    companion object : UIViewMeta() {
        @BetaInteropApi
        override fun layerClass() = CAMetalLayer
    }

    private val device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice()
            ?: throw IllegalStateException("Metal is not supported on this system")
    private val metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    private var drawableWidth: CGFloat = 0.0
    private var drawableHeight: CGFloat = 0.0
    private val redrawer: MetalRedrawer = MetalRedrawer(
        metalLayer,
        callbacks = object : MetalRedrawerCallbacks {
            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                renderDelegate.onRender(
                    canvas,
                    width = drawableWidth.toInt(),
                    height = drawableHeight.toInt(),
                    nanoTime = targetTimestamp.toNanoSeconds()
                )
            }

            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                this@MetalView.retrieveInteropTransaction()
        }
    )

    /**
     * @see [MetalRedrawer.canBeOpaque]
     */
    var canBeOpaque by redrawer::canBeOpaque

    /**
     * @see [MetalRedrawer.needsProactiveDisplayLink]
     */
    var needsProactiveDisplayLink by redrawer::needsProactiveDisplayLink

    init {
        userInteractionEnabled = false

        metalLayer.also {
            // Workaround for cinterop issue
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            it.device = device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            it.backgroundColor = UIColor.clearColor.CGColor
            it.framebufferOnly = false
        }
    }

    fun needRedraw() = redrawer.needRedraw()

    var isForcedToPresentWithTransactionEveryFrame by redrawer::isForcedToPresentWithTransactionEveryFrame

    fun dispose() {
        redrawer.dispose()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        val window = window ?: return

        val screen = window.screen
        contentScaleFactor = screen.scale
        redrawer.maximumFramesPerSecond = screen.maximumFramesPerSecond

        updateMetalLayerSize()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        updateMetalLayerSize()
    }

    private fun updateMetalLayerSize() {
        if (window == null || CGRectIsEmpty(bounds)) {
            return
        }
        bounds.useContents {
            drawableWidth = size.width * contentScaleFactor
            drawableHeight = size.height * contentScaleFactor
        }

        // If drawableSize is zero in any dimension it means that it's a first layout
        // we need to synchronously dispatch first draw and block until it's presented
        // so user doesn't have a flicker
        val needsSynchronousDraw = metalLayer.drawableSize.useContents {
            width == 0.0 || height == 0.0
        }

        metalLayer.drawableSize = CGSizeMake(drawableWidth, drawableHeight)

        if (needsSynchronousDraw) {
            redrawer.drawSynchronously()
        }
    }

    override fun canBecomeFirstResponder() = false
}

private fun NSTimeInterval.toNanoSeconds(): Long {
    // The calculation is split in two instead of
    // `(targetTimestamp * 1e9).toLong()`
    // to avoid losing precision for fractional part
    val integral = floor(this)
    val fractional = this - integral
    val secondsToNanos = 1_000_000_000L
    val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()
    return nanos
}
