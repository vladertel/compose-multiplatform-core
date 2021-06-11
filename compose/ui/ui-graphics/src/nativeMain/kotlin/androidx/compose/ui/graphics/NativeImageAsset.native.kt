/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.jetbrains.skiko.skia.native.*
import kotlinx.cinterop.*
import kotlin.math.abs
import kotlin.math.min

/**
 * Create an [ImageBitmap] from the given [Bitmap]. Note this does
 * not create a copy of the original [Bitmap] and changes to it
 * will modify the returned [ImageBitmap]
 */
//fun Bitmap.asImageBitmap(): ImageBitmap = DesktopImageBitmap(this)

/**
 * Create an [ImageBitmap] from the given [Image].
 */
fun Image.asImageBitmap(): ImageBitmap = NativeImageBitmap(toBitmap())


private fun Image.toBitmap(): Bitmap {
    val bitmap = Bitmap()
    // TODO: The last arg should be null, but Skia plugin doesn't let me pass null here.
    // bitmap.allocPixels(SkImageInfo.MakeN32(width(), height(), kPremul_SkAlphaType, null))
    bitmap.allocPixels(SkImageInfo.MakeN32(width(), height(), kPremul_SkAlphaType, SkColorSpace.MakeSRGB()))
    val canvas = org.jetbrains.skiko.skia.native.Canvas(bitmap)
    canvas.drawImage(this, 0f, 0f)
    bitmap.setImmutable()
    return bitmap
}


internal actual fun ActualImageBitmap(
    width: Int,
    height: Int,
    config: ImageBitmapConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace
): ImageBitmap {
    val colorType = config.toSkiaColorType()
    val alphaType = if (hasAlpha) kPremul_SkAlphaType else kOpaque_SkAlphaType
    val skiaColorSpace = colorSpace.toSkiaColorSpace()
    val colorInfo = ColorInfo(colorType, alphaType, skiaColorSpace)
    // TODO: have non-Sk counterparts here.
    val imageInfo = SkImageInfo.Make(width, height, colorType, alphaType, SkColorSpace.MakeSRGB())
    val bitmap = Bitmap()
    bitmap.allocPixels(imageInfo)
    return NativeImageBitmap(bitmap)
}

/*
/**
 * Create an [ImageBitmap] from an image file stored in resources for the application
 *
 * @param path path to the image file
 *
 * @return Loaded image file represented as an [ImageBitmap]
 */
fun imageFromResource(path: String): ImageBitmap =
    Image.makeFromEncoded(loadResource(path)).asImageBitmap()

private fun loadResource(path: String): ByteArray {
    val resource = Thread.currentThread().contextClassLoader.getResource(path)
    requireNotNull(resource) { "Resource $path not found" }
    return resource.readBytes()
}
*/

fun ImageBitmap.asSkiaBitmap(): Bitmap =
    when (this) {
        is NativeImageBitmap -> bitmap
        else -> error("Unable to obtain org.jetbrains.skiko.skia.native.Image")
    }

private class NativeImageBitmap(val bitmap: Bitmap) : ImageBitmap {
    override val colorSpace = bitmap.colorSpace().toComposeColorSpace()
    override val config = bitmap.colorType().toComposeConfig()
    override val hasAlpha = !bitmap.isOpaque()
    override val height get() = bitmap.height()
    override val width get() = bitmap.width()
    override fun prepareToDraw() = Unit

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        // similar to https://cs.android.com/android/platform/superproject/+/42c50042d1f05d92ecc57baebe3326a57aeecf77:frameworks/base/graphics/java/android/graphics/Bitmap.java;l=2007
        val lastScanline: Int = bufferOffset + (height - 1) * stride
        require(startX >= 0 && startY >= 0)
        require(width > 0 && startX + width <= this.width)
        require(height > 0 && startY + height <= this.height)
        require(abs(stride) >= width)
        require(bufferOffset >= 0 && bufferOffset + width <= buffer.size)
        require(lastScanline >= 0 && lastScanline + width <= buffer.size)

        val imageInfo = SkImageInfo.Make(width, height, kBGRA_8888_SkColorType, kUnpremul_SkAlphaType, SkColorSpace.MakeSRGB())
        val bytesPerPixel = 4
        val rowBytes = stride * bytesPerPixel
        val bytes = ByteArray(min(height, bitmap.height()-startY) * rowBytes) {0}
        bytes.usePinned { pinned ->
            bitmap.readPixels(imageInfo, pinned.addressOf(0), rowBytes.toULong(), startX, startY)!!
        }

        TODO("implement this byte buffer transformation")
/*
        ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN) // to return ARGB
            .asIntBuffer()
            .get(buffer, bufferOffset, bytes.size / bytesPerPixel)

 */
    }
}

// TODO(demin): [API] maybe we should use:
//  `else -> throw UnsupportedOperationException()`
//  in toSkijaColorType/toComposeConfig/toComposeColorSpace/toSkijaColorSpace
//  see [https://android-review.googlesource.com/c/platform/frameworks/support/+/1429835/comment/c219501b_63c3d1fe/]

private fun ImageBitmapConfig.toSkiaColorType() = when (this) {
    ImageBitmapConfig.Argb8888 -> kN32_SkColorType
    ImageBitmapConfig.Alpha8 -> error("figure out ColorType.ALPHA_8")// ColorType.ALPHA_8
    ImageBitmapConfig.Rgb565 -> kRGB_565_SkColorType
    ImageBitmapConfig.F16 -> kRGBA_F16_SkColorType
    else -> kN32_SkColorType
}

private fun SkColorType.toComposeConfig() = when (this) {
    kN32_SkColorType -> ImageBitmapConfig.Argb8888
    // ColorType.ALPHA_8 -> ImageBitmapConfig.Alpha8
    kRGB_565_SkColorType -> ImageBitmapConfig.Rgb565
    kRGBA_F16_SkColorType -> ImageBitmapConfig.F16
    else -> ImageBitmapConfig.Argb8888
}

private fun org.jetbrains.skiko.skia.native.ColorSpace?.toComposeColorSpace(): ColorSpace {
    return when {
        this?.isSRGB() == true -> ColorSpaces.Srgb
        else -> TODO("implement native toComposeColorSpace()")
    }
    /*
    return when (this) {
        org.jetbrains.skiko.skia.native.ColorSpace.getSRGB() -> ColorSpaces.Srgb
        org.jetbrains.skiko.skia.native.ColorSpace.getSRGBLinear() -> ColorSpaces.LinearSrgb
        org.jetbrains.skiko.skia.native.ColorSpace.getDisplayP3() -> ColorSpaces.DisplayP3
        else -> ColorSpaces.Srgb
    }
     */
}

// TODO(demin): support all color spaces.
//  to do this we need to implement SkColorSpace::MakeRGB in skija
private fun ColorSpace.toSkiaColorSpace(): org.jetbrains.skiko.skia.native.ColorSpace {
    return when (this) {
        ColorSpaces.Srgb -> org.jetbrains.skiko.skia.native.ColorSpace.MakeSRGB()
        ColorSpaces.LinearSrgb -> TODO("figure out LinearSrgb") // org.jetbrains.skiko.skia.native.ColorSpace.getSRGBLinear()
        ColorSpaces.DisplayP3 -> TODO("figure out DisplayP3") // org.jetbrains.skiko.skia.native.ColorSpace.getDisplayP3()
        else -> org.jetbrains.skiko.skia.native.ColorSpace.MakeSRGB()
    }
}
