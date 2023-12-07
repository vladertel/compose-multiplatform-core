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

package androidx.compose.ui.scene.skia

import java.awt.Component
import javax.accessibility.Accessible
import javax.swing.JComponent
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi

internal interface SkiaLayerComponent {
    val contentComponent: JComponent
    val focusComponent: Component

    val renderApi: GraphicsApi
    val interopBlendingSupported: Boolean

    var transparency: Boolean
    var fullscreen: Boolean
    val windowHandle: Long

    val clipComponents: MutableList<ClipRectangle>

    fun dispose()

    fun requestNativeFocusOnAccessible(accessible: Accessible)
    fun onComposeSceneInvalidate()
    fun onRenderApiChanged(action: () -> Unit)
}