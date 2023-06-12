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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.density
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.im.InputMethodRequests
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.swing.JComponent
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.swing.SkiaSwingLayer
import org.jetbrains.skiko.swing.SkiaSwingLayerComponent

internal class ComposeSwingLayer(
    private val skiaLayerAnalytics: SkiaLayerAnalytics
) : ComposeLayer() {
    private val _component = ComponentImpl()
    val component: SkiaSwingLayerComponent get() = _component

    override val componentLayer: JComponent
        get() = _component
    override val focusComponentDelegate: Component
        get() = _component

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        // TODO: support a11y
    }

    override fun onComposeInvalidation() {
        _component.repaint()
    }

    init {
        attachComposeToComponent()
    }

    override fun disposeComponentLayer() {
        _component.dispose()
    }

    @OptIn(ExperimentalSkikoApi::class)
    private inner class ComponentImpl :
        SkiaSwingLayer(skikoView = skikoView, analytics = skiaLayerAnalytics) {
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
            this@ComposeSwingLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else Dimension(
                (this@ComposeSwingLayer.scene.contentSize.width / density.density).toInt(),
                (this@ComposeSwingLayer.scene.contentSize.height / density.density).toInt()
            )
        }

        private fun resetDensity() {
            if (this@ComposeSwingLayer.scene.density != density) {
                this@ComposeSwingLayer.scene.density = density
                updateSceneSize()
            }
        }

        private fun refreshWindowFocus() {
            platform.windowInfo.isWindowFocused = window?.isFocused ?: false
            keyboardModifiersRequireUpdate = true
        }

        override fun getAccessibleContext(): AccessibleContext? {
            return sceneAccessible.accessibleContext
        }
    }
}