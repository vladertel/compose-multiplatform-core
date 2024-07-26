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

package androidx.compose.ui.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.util.fastForEach
import kotlinx.cinterop.refTo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRef
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRef
import platform.UIKit.UIColor
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.kCGImageByteOrder32Little
import platform.CoreGraphics.kCGImagePixelFormatPacked
import platform.UIKit.UIImage

/**
 * Convert [androidx.compose.ui.graphics.Color] to iOS UIKit [UIColor]
 * Assumes that source color is in sRGB color space.
 */
internal fun Color.toUIColor() = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)

internal sealed interface CFScopeReleasable {
    fun release()

    data class Image(val image: CGImageRef) : CFScopeReleasable {
        override fun release() {
            CGImageRelease(image)
        }
    }

    data class ColorSpace(val colorSpace: platform.CoreGraphics.CGColorSpaceRef) :
        CFScopeReleasable {
        override fun release() {
            CGColorSpaceRelease(colorSpace)
        }
    }

    data class Context(val context: platform.CoreGraphics.CGContextRef) : CFScopeReleasable {
        override fun release() {
            CGContextRelease(context)
        }
    }
}

internal class CGReleaseScope {
    private val items = mutableListOf<CFScopeReleasable>()

    fun release() {
        items.reversed().fastForEach { it.release() }
    }

    private fun add(item: CFScopeReleasable) {
        items.add(item)
    }

    fun CGContextRef.releasedAfterScopeEnds(): CGContextRef {
        add(CFScopeReleasable.Context(this))
        return this
    }

    fun CGColorSpaceRef.releasedAfterScopeEnds(): CGColorSpaceRef {
        add(CFScopeReleasable.ColorSpace(this))
        return this
    }
}

internal fun <R> withCGReleaseScope(block: CGReleaseScope.() -> R): R {
    val scope = CGReleaseScope()
    return try {
        scope.block()
    } finally {
        scope.release()
    }
}


/**
 * Creates a iOS CoreGraphics [CGImageRef] with the contents of [ImageBitmap].
 * @return A retained [CGImageRef] that needs to be manually released via [CGImageRelease]
 */
internal fun ImageBitmap.toCGImage(): CGImageRef? = withCGReleaseScope {
    if (config != ImageBitmapConfig.Argb8888) {
        throw NotImplementedError("Only ImageBitmapConfig.Argb8888 is supported")
    }

    val buffer = IntArray(width * height)

    readPixels(buffer)

    val hexLetters = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"
    )
    val samplesPerWidth = 10
    val samplesPerHeight =
        (height.toFloat() / width.toFloat() * samplesPerWidth.toFloat()).toInt().coerceAtLeast(1)

    for (ySample in 0 until samplesPerHeight) {
        for (xSample in 0 until samplesPerWidth) {
            val y = (ySample.toFloat() / samplesPerHeight.toFloat() * height.toFloat()).toInt()
                .coerceAtMost(height - 1)
            val x = (xSample.toFloat() / samplesPerWidth.toFloat() * width.toFloat()).toInt()
                .coerceAtMost(width - 1)

            val color = buffer[y * width + x]

            fun Int.toHexString(): String {
                val first = this / 16
                val second = this % 16
                return hexLetters[first] + hexLetters[second]
            }

            val alpha = (color shr 24 and 0xFF) / 255.0
            val red = (color shr 16 and 0xFF).toHexString()
            val green = (color shr 8 and 0xFF).toHexString()
            val blue = (color and 0xFF).toHexString()

            print("#$red$green$blue($alpha) ")
        }
        println()
    }

    val colorSpace =
        CGColorSpaceCreateDeviceRGB()?.releasedAfterScopeEnds() ?: return@withCGReleaseScope null

    val bitmapInfo =
        CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or kCGImageByteOrder32Little

    val context = CGBitmapContextCreate(
        data = buffer.refTo(0),
        width = width.toULong(),
        height = height.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (4 * width).toULong(),
        space = colorSpace,
        bitmapInfo = bitmapInfo
    )?.releasedAfterScopeEnds() ?: return@withCGReleaseScope null

    val cgImage = CGBitmapContextCreateImage(context) // must be released by the user
    return@withCGReleaseScope cgImage
}

/**
 * Creates a iOS UIKit [UIImage] with the contents of [ImageBitmap].
 */
internal fun ImageBitmap.toUIImage(): UIImage? {
    val cgImage = toCGImage() ?: return null
    val uiImage = UIImage.imageWithCGImage(cgImage)
    CGImageRelease(cgImage)
    return uiImage
}
