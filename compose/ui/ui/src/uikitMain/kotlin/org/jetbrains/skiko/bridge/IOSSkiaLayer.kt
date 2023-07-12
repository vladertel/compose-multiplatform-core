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

import kotlin.system.getTimeNanos
import kotlinx.cinterop.useContents
import platform.UIKit.UIView
import org.jetbrains.skiko.*
import org.jetbrains.skia.*

class IOSSkiaLayer : SkiaLayerInterface {
    var needRedrawCallback: () -> Unit = { }
    var detachCallback: () -> Unit = { }

    val contentScale: Float
        get() = view!!.contentScaleFactor.toFloat()

    override fun needRedraw() {
        needRedrawCallback()
    }

    val width: Float
       get() = view!!.frame.useContents {
           return@useContents size.width.toFloat()
       }

    val height: Float
        get() = view!!.frame.useContents {
            return@useContents size.height.toFloat()
        }

    var view: UIView? = null
    // We need to keep reference to gesturesDetector as Objective-C will only keep weak reference here.
    private var gesturesDetector = SkikoGesturesDetector(this)

    private var gesturesToListen: Array<SkikoGestureEventKind>? = null
        set(value) {
            field = value
            initGestures()
        }

    fun initGestures() {
        gesturesDetector.setGesturesToListen(gesturesToListen)
    }

    private var isDisposed = false

    override fun detach() {
        if (!isDisposed) {
            detachCallback()
            isDisposed = true
        }
    }
    override var skikoView: SkikoView? = null

    internal fun draw(canvas: Canvas) {
        check(!isDisposed) { "SkiaLayer is disposed" }
        val (w, h) = view!!.frame.useContents {
            size.width to size.height
        }
        val pictureWidth = (w.toFloat() * contentScale).coerceAtLeast(0.0F)
        val pictureHeight = (h.toFloat() * contentScale).coerceAtLeast(0.0F)

        skikoView?.onRender(canvas, pictureWidth.toInt(), pictureHeight.toInt(), getTimeNanos())
    }
}
