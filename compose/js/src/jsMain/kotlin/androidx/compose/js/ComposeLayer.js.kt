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

package androidx.compose.js
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.JsOwner
import androidx.compose.ui.platform.JsOwners
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Density
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.jetbrains.skiko.wasm.api.CanvasRenderer
import org.w3c.dom.HTMLCanvasElement

class ComposeLayer {

    private var isDisposed = false

    private var density: Density = Density(1f, 1f)

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val owners = JsOwners(
        coroutineScope = coroutineScope,
    )

    private var content: (@Composable () -> Unit)? = null
    private var composition: Composition? = null
    private var parentComposition: CompositionContext? = null

    private var owner: JsOwner? = null

    init {
        val cnvs1 = document.getElementById("cnvs1") as HTMLCanvasElement
        val cr = object : CanvasRenderer(
            cnvs1
        ) {
            override fun drawFrame(currentTimestamp: Double) {
                owners.onFrame(canvas, cnvs1.width, cnvs1.height, currentTimestamp.toLong())
            }
        }.draw()
    }

    internal fun setContent(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        check(!isDisposed)
        check(this.content == null) { "Cannot set content twice" }
        this.content = content
        this.parentComposition = parentComposition
        // We can't create NativeOwner now, because we don't know density yet.
        // We will know density only after SkiaLayer will be visible.
        initOwner()
    }


    private fun initOwner() {
        check(!isDisposed)
        if (owner == null && content != null) {
            println("init Owner")
            owner = JsOwner(owners, density)
            composition = owner!!.setContent(parent = parentComposition, content = content!!)
        }
    }

    fun dispose() {
        check(!isDisposed)
        composition?.dispose()
        owner?.dispose()

        // events.cancel()
        coroutineScope.cancel()
//        wrapped.dispose()

        isDisposed = true
    }

}
