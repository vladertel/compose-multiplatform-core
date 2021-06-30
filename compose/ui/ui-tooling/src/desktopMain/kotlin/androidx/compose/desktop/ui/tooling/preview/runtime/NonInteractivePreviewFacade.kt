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

package androidx.compose.desktop.ui.tooling.preview.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.tooling.CommonPreviewUtils
import androidx.compose.ui.unit.Density

// used in Compose For MPP Intellij plugin through reflection
@Suppress("unused")
internal class NonInteractivePreviewFacade {
    companion object {
        @JvmStatic
        fun render(fqName: String, width: Int, height: Int, scale: Double?): ByteArray {
            val className = fqName.substringBeforeLast(".")
            val methodName = fqName.substringAfterLast(".")
            val density = scale?.let { Density(it.toFloat()) } ?: Density(1f)
            val window = TestComposeWindow(width = width, height = height, density = density)
            window.setContent @Composable {
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                CommonPreviewUtils.invokeComposableViaReflection(
                    className,
                    methodName,
                    currentComposer
                )
            }
            return window.surface.makeImageSnapshot().encodeToData()!!.bytes
        }
    }
}