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

package org.jetbrains.skiko.bridge

import androidx.compose.ui.unit.IntSize
import kotlin.system.getTimeNanos
import kotlinx.cinterop.useContents
import platform.UIKit.UIView
import org.jetbrains.skiko.*
import org.jetbrains.skia.*

// TODO: this class feels like a gimmick, candidate for removal
class IOSSkiaLayer : SkiaLayerInterface {
    var needRedrawCallback: () -> Unit = { }
    var detachCallback: () -> Unit = { }

    override fun needRedraw() {
        needRedrawCallback()
    }

    private var isDisposed = false

    override fun detach() {
        if (!isDisposed) {
            detachCallback()
            isDisposed = true
        }
    }
    override var skikoView: SkikoView? = null
}
