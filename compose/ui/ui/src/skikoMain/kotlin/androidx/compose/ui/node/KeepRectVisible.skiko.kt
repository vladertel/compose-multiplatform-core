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

package androidx.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private class OffsetAnimationState {
    var current by mutableStateOf(IntOffset.Zero)
        private set
    var start by mutableStateOf(IntOffset.Zero)
    var progress by mutableStateOf(1f)

    fun updateCurrent(end: IntOffset) {
        current = start + (end - start) * progress
    }
}

@Composable
internal fun KeepRectVisible(
    trackingRect: Rect?,
    size: IntSize?,
    insets: PlatformInsets,
    content: @Composable () -> Unit
) {
    val animationState = remember { OffsetAnimationState() }
    LaunchedEffect(trackingRect, insets) {
        animationState.start = animationState.current
        withAnimationProgress(FOCUS_CHANGE_ANIMATION_DURATION) {
            animationState.progress = it
        }
    }

    Layout(
        content = content,
        measurePolicy = { measurables, constraints ->
            val endOffset = adjustedOffset(trackingRect, size, animationState.current, insets)

            // Intentionally update state within composition to trigger second measure and
            // layout because focus rect may be miscalculated due to simultaneous offset and
            // window insets changes.
            animationState.updateCurrent(endOffset)

            val placeables = measurables.map { it.measure(constraints) }
            layout(
                placeables.maxOfOrNull { it.width } ?: constraints.minWidth,
                placeables.maxOfOrNull { it.height } ?: constraints.minHeight
            ) {
                placeables.forEach {
                    it.place(animationState.current)
                }
            }
        }
    )
}

private suspend fun withAnimationProgress(duration: Duration, update: (Float) -> Unit) {
    fun easeInOutProgress(progress: Float) = if (progress < 0.5) {
        2 * progress * progress
    } else {
        (-2 * progress * progress) + (4 * progress) - 1
    }

    update(0f)

    var firstFrameTime = 0L
    var progressDuration = Duration.ZERO
    while (progressDuration < duration) {
        withFrameNanos { frameTime ->
            if (firstFrameTime == 0L) {
                firstFrameTime = frameTime
            }
            progressDuration = (frameTime - firstFrameTime).nanoseconds
            val progress = easeInOutProgress(
                min(1.0, progressDuration / duration).toFloat()
            )
            update(progress)
        }
    }
}

private fun Density.adjustedOffset(
    focusedRect: Rect?,
    size: IntSize?,
    currentOffset: IntOffset,
    insets: PlatformInsets
): IntOffset {
    focusedRect ?: return IntOffset.Zero
    size ?: return IntOffset.Zero
    if (insets == PlatformInsets.Zero) {
        return IntOffset.Zero
    }

    return IntOffset(
        x = directionalFocusOffset(
            contentSize = size.width.toFloat(),
            contentInsetStart = insets.left.toPx(),
            contentInsetEnd = insets.right.toPx(),
            focusStart = focusedRect.left - currentOffset.x,
            focusEnd = focusedRect.right - currentOffset.x
        ),
        y = directionalFocusOffset(
            contentSize = size.height.toFloat(),
            contentInsetStart = insets.top.toPx(),
            contentInsetEnd = insets.bottom.toPx(),
            focusStart = focusedRect.top - currentOffset.y,
            focusEnd = focusedRect.bottom - currentOffset.y
        )
    )
}

private fun directionalFocusOffset(
    contentSize: Float,
    contentInsetStart: Float,
    contentInsetEnd: Float,
    focusStart: Float,
    focusEnd: Float
): Int {
    val hiddenFromPart = contentInsetStart - max(focusStart, 0f)
    val hiddenToPart = contentInsetEnd - contentSize + min(focusEnd, contentSize)

    return if (hiddenFromPart >= 0 && hiddenToPart >= 0) {
        0
    } else if (hiddenToPart < 0) {
        max(0f, min(hiddenFromPart, -hiddenToPart)).roundToInt()
    } else {
        min(0f, max(hiddenFromPart, -hiddenToPart)).roundToInt()
    }
}

private val FOCUS_CHANGE_ANIMATION_DURATION = 0.15.seconds
