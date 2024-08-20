/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.uikit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.uiContentSizeCategoryToFontScaleMap
import platform.UIKit.UIColor
import platform.UIKit.UIContentSizeCategoryUnspecified
import platform.UIKit.UIScreen
import platform.UIKit.UIView

internal val UIView.density: Density
    get() {
        // TODO: It's a code smell that we have to retrive a default UIScreen here.
        //   We probably should reorder the code so that density is either injected from outside
        //   or view is attached to a window before this is called.
        val screen = window?.screen
            ?: UIScreen.mainScreen

        val contentSizeCategory = traitCollection.preferredContentSizeCategory ?: UIContentSizeCategoryUnspecified

        return Density(
            density = screen.nativeScale.toFloat(),
            fontScale = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        )
    }

internal fun Color.toUIColor(): UIColor? =
    if (this == Color.Unspecified) {
        null
    } else {
        UIColor(
            red = red.toDouble(),
            green = green.toDouble(),
            blue = blue.toDouble(),
            alpha = alpha.toDouble(),
        )
    }
