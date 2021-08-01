/*
 * Copyright 2019 The Android Open Source Project
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

import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.annotation.RequiresApi

actual typealias PlatformPaint = android.graphics.Paint

actual fun Paint(): Paint = AndroidPaint()

class AndroidPaint : Paint {

    private var internalPaint = makePlatformPaint()
    private var _blendMode = BlendMode.SrcOver
    private var internalShader: Shader? = null
    private var internalColorFilter: ColorFilter? = null

    override fun asFrameworkPaint(): PlatformPaint = internalPaint

    override var alpha: Float
        get() = internalPaint.getPlatformAlpha()
        set(value) {
            internalPaint.setPlatformAlpha(value)
        }

    override var isAntiAlias: Boolean
        get() = internalPaint.getPlatformAntiAlias()
        set(value) {
            internalPaint.setPlatformAntiAlias(value)
        }

    override var color: Color
        get() = internalPaint.getPlatformColor()
        set(color) {
            internalPaint.setPlatformColor(color)
        }

    override var blendMode: BlendMode
        get() = _blendMode
        set(value) {
            _blendMode = value
            internalPaint.setPlatformBlendMode(value)
        }

    override var style: PaintingStyle
        get() = internalPaint.getPlatformStyle()
        set(value) {
            internalPaint.setPlatformStyle(value)
        }

    override var strokeWidth: Float
        get() = internalPaint.getPlatformStrokeWidth()
        set(value) {
            internalPaint.setPlatformStrokeWidth(value)
        }

    override var strokeCap: StrokeCap
        get() = internalPaint.getPlatformStrokeCap()
        set(value) {
            internalPaint.setPlatformStrokeCap(value)
        }

    override var strokeJoin: StrokeJoin
        get() = internalPaint.getPlatformStrokeJoin()
        set(value) {
            internalPaint.setPlatformStrokeJoin(value)
        }

    override var strokeMiterLimit: Float
        get() = internalPaint.getPlatformStrokeMiterLimit()
        set(value) {
            internalPaint.setPlatformStrokeMiterLimit(value)
        }

    // TODO(ianh): verify that the image drawing methods actually respect this
    override var filterQuality: FilterQuality
        get() = internalPaint.getPlatformFilterQuality()
        set(value) {
            internalPaint.setPlatformFilterQuality(value)
        }

    override var shader: Shader?
        get() = internalShader
        set(value) {
            internalShader = value
            internalPaint.setPlatformShader(internalShader)
        }

    override var colorFilter: ColorFilter?
        get() = internalColorFilter
        set(value) {
            internalColorFilter = value
            internalPaint.setPlatformColorFilter(value)
        }

    override var pathEffect: PathEffect? = null
        set(value) {
            internalPaint.setPlatformPathEffect(value)
            field = value
        }
}

internal fun makePlatformPaint() =
    android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG or
            android.graphics.Paint.DITHER_FLAG or
            android.graphics.Paint.FILTER_BITMAP_FLAG
    )

internal fun PlatformPaint.setPlatformBlendMode(mode: BlendMode) {
    if (Build.VERSION.SDK_INT >= 29) {
        // All blend modes supported in Q
        WrapperVerificationHelperMethods.setBlendMode(this, mode)
    } else {
        // Else fall back on platform alternatives
        this.xfermode = PorterDuffXfermode(mode.toPorterDuffMode())
    }
}

internal fun PlatformPaint.setPlatformColorFilter(value: ColorFilter?) {
    colorFilter = value?.asAndroidColorFilter()
}

internal fun PlatformPaint.getPlatformAlpha() = this.alpha / 255f

internal fun PlatformPaint.setPlatformAlpha(value: Float) {
    this.alpha = kotlin.math.round(value * 255.0f).toInt()
}

internal fun PlatformPaint.getPlatformAntiAlias(): Boolean = this.isAntiAlias

internal fun PlatformPaint.setPlatformAntiAlias(value: Boolean) {
    this.isAntiAlias = value
}

internal fun PlatformPaint.getPlatformColor(): Color = Color(this.color)

internal fun PlatformPaint.setPlatformColor(value: Color) {
    this.color = value.toArgb()
}

internal fun PlatformPaint.setPlatformStyle(value: PaintingStyle) {
    // TODO(njawad): Platform also supports Paint.Style.FILL_AND_STROKE)
    this.style = when (value) {
        PaintingStyle.Stroke -> android.graphics.Paint.Style.STROKE
        else -> android.graphics.Paint.Style.FILL
    }
}

internal fun PlatformPaint.getPlatformStyle() = when (this.style) {
    android.graphics.Paint.Style.STROKE -> PaintingStyle.Stroke
    else -> PaintingStyle.Fill
}

internal fun PlatformPaint.getPlatformStrokeWidth(): Float =
    this.strokeWidth

internal fun PlatformPaint.setPlatformStrokeWidth(value: Float) {
    this.strokeWidth = value
}

internal fun PlatformPaint.getPlatformStrokeCap(): StrokeCap = when (this.strokeCap) {
    android.graphics.Paint.Cap.BUTT -> StrokeCap.Butt
    android.graphics.Paint.Cap.ROUND -> StrokeCap.Round
    android.graphics.Paint.Cap.SQUARE -> StrokeCap.Square
    else -> StrokeCap.Butt
}

internal fun PlatformPaint.setPlatformStrokeCap(value: StrokeCap) {
    this.strokeCap = when (value) {
        StrokeCap.Square -> android.graphics.Paint.Cap.SQUARE
        StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
        StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
        else -> android.graphics.Paint.Cap.BUTT
    }
}

internal fun PlatformPaint.getPlatformStrokeJoin(): StrokeJoin =
    when (this.strokeJoin) {
        android.graphics.Paint.Join.MITER -> StrokeJoin.Miter
        android.graphics.Paint.Join.BEVEL -> StrokeJoin.Bevel
        android.graphics.Paint.Join.ROUND -> StrokeJoin.Round
        else -> StrokeJoin.Miter
    }

internal fun PlatformPaint.setPlatformStrokeJoin(value: StrokeJoin) {
    this.strokeJoin = when (value) {
        StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
        StrokeJoin.Bevel -> android.graphics.Paint.Join.BEVEL
        StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
        else -> android.graphics.Paint.Join.MITER
    }
}

internal fun PlatformPaint.getPlatformStrokeMiterLimit(): Float =
    this.strokeMiter

internal fun PlatformPaint.setPlatformStrokeMiterLimit(value: Float) {
    this.strokeMiter = value
}

internal fun PlatformPaint.getPlatformFilterQuality(): FilterQuality =
    if (!this.isFilterBitmap) {
        FilterQuality.None
    } else {
        // TODO b/162284721 (njawad): Align with Framework APIs)
        // Framework only supports bilinear filtering which maps to FilterQuality.low
        // FilterQuality.medium and FilterQuailty.high refer to a combination of
        // bilinear interpolation, pyramidal parameteric prefiltering (mipmaps) as well as
        // bicubic interpolation respectively
        FilterQuality.Low
    }

internal fun PlatformPaint.setPlatformFilterQuality(value: FilterQuality) {
    this.isFilterBitmap = value != FilterQuality.None
}

internal fun PlatformPaint.setPlatformShader(value: Shader?) {
    this.shader = value
}

internal fun PlatformPaint.setPlatformPathEffect(value: PathEffect?) {
    this.pathEffect = (value as AndroidPathEffect?)?.nativePathEffect
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal object WrapperVerificationHelperMethods {
    @androidx.annotation.DoNotInline
    fun setBlendMode(paint: PlatformPaint, mode: BlendMode) {
        paint.blendMode = mode.toAndroidBlendMode()
    }
}