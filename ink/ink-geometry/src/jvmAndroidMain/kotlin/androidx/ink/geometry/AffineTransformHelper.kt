/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.ink.nativeloader.NativeLoader

/** Helper functions for AffineTransform. */
internal object AffineTransformHelper {

    init {
        NativeLoader.load()
    }

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun nativeApplyParallelogram(
        affineTransformA: Float,
        affineTransformB: Float,
        affineTransformC: Float,
        affineTransformD: Float,
        affineTransformE: Float,
        affineTransformF: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramRotation: Float,
        parallelogramShearFactor: Float,
        out: MutableParallelogram,
    )
}
