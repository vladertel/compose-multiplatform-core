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

package androidx.compose.ui.awt

import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.density
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.accessibility.Accessible
import javax.swing.JComponent
import javax.swing.SwingUtilities
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class ComposeWindowLayer(
    private val skiaLayerAnalytics: SkiaLayerAnalytics
) : ComposeLayer() {
    private val _component = ComponentImpl()
    val component: SkiaLayer get() = _component

    override val componentLayer: JComponent
        get() = _component
    override val focusComponentDelegate: Component
        get() = _component.canvas

    var compositionLocalContext: CompositionLocalContext? by scene::compositionLocalContext

    init {
        _component.skikoView = skikoView
        attachComposeToComponent()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        _component.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeInvalidation() {
        _component.needRedraw()
    }

    override fun disposeComponentLayer() {
        _component.dispose()
    }

    private inner class ComponentImpl :
        SkiaLayer(
            externalAccessibleFactory = { sceneAccessible },
            analytics = skiaLayerAnalytics
        ),
        Accessible {
        private var window: Window? = null
        private var windowListener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) = refreshWindowFocus()
            override fun windowLostFocus(e: WindowEvent) = refreshWindowFocus()
        }

        override fun addNotify() {
            super.addNotify()
            resetDensity()
            initContent()
            updateSceneSize()
            window = SwingUtilities.getWindowAncestor(this)
            window?.addWindowFocusListener(windowListener)
            refreshWindowFocus()
        }

        override fun removeNotify() {
            window?.removeWindowFocusListener(windowListener)
            window = null
            refreshWindowFocus()
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            resetDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            updateSceneSize()
        }

        private fun updateSceneSize() {
            this@ComposeWindowLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else Dimension(
                (this@ComposeWindowLayer.scene.contentSize.width / density.density).toInt(),
                (this@ComposeWindowLayer.scene.contentSize.height / density.density).toInt()
            )
        }

        private fun resetDensity() {
            if (this@ComposeWindowLayer.scene.density != density) {
                this@ComposeWindowLayer.scene.density = density
                updateSceneSize()
            }
        }

        private fun refreshWindowFocus() {
            this@ComposeWindowLayer.platform.windowInfo.isWindowFocused = window?.isFocused ?: false
            keyboardModifiersRequireUpdate = true
        }
    }
}
