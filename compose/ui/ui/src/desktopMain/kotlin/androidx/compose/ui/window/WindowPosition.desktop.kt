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

package androidx.compose.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs an [WindowPosition] from [x] and [y] [Dp] values.
 */
fun WindowPosition(
    /**
     * The horizontal position of the window in [Dp].
     */
    x: Dp,

    /**
     * The vertical position of the window in [Dp].
     */
    y: Dp
) = WindowPosition(packFloats(x.value, y.value))

/**
 * Position of the window or dialog on the screen in [Dp].
 */
@Immutable
inline class WindowPosition internal constructor(@PublishedApi internal val packedValue: Long) {
    /**
     * The horizontal position of the window in [Dp].
     */
    @Stable
    val x: Dp
        get() = unpackFloat1(packedValue).dp

    /**
     * The vertical position of the window in [Dp].
     */
    @Stable
    val y: Dp
        get() = unpackFloat2(packedValue).dp

    @Stable
    operator fun component1(): Dp = x

    @Stable
    operator fun component2(): Dp = y

    /**
     * Returns a copy of this [WindowPosition] instance optionally overriding the
     * [x] or [y] parameter.
     */
    fun copy(x: Dp = this.x, y: Dp = this.y): WindowPosition = WindowPosition(x, y)

    @Stable
    override fun toString() = "($x, $y)"

    /**
     * `true` if the window position in initial state, and should be
     * determined once the window will be added to the screen.
     *
     * Initial position of the window depends on the platform, but usually every new window
     * will be positioned in a cascade mode.
     */
    val isInitial get() = x.isUnspecified && y.isUnspecified

    companion object {
        /**
         * Constant that means that position of the window is in initial state, and should be
         * determined once the window will be added to the screen.
         *
         * Initial position of the window depends on the platform, but usually every new window
         * will be positioned in a cascade mode.
         *
         * Initial position can be overridden by the user, for example window can be aligned in
         * the center of the screen. Initial alignment of the window is determined in the
         * appropriate Composable function (for example, in [Window] or [Dialog]).
         */
        val Initial = WindowPosition(Dp.Unspecified, Dp.Unspecified)
    }
}