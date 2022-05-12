/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.DefaultViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalCoroutinesApi::class)
class MouseCombinedClickableTest {

    private fun testClick(
        filter: PointerFilterScope.() -> Boolean,
        pressButtons: PointerButtons,
    ) = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var clicksCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .combinedClickable(filter = filter) {
                        clicksCount++
                    }.size(10.dp, 20.dp)
            )
        }

        val upButtons = PointerButtons()
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = pressButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
        assertThat(clicksCount).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = pressButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), buttons = upButtons)
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun primaryClicks() = testClick(
        filter = { button.isPrimary },
        pressButtons = PointerButtons(isPrimaryPressed = true)
    )

    @Test
    fun secondaryClicks() = testClick(
        filter = { button.isSecondary },
        pressButtons = PointerButtons(isSecondaryPressed = true)
    )

    private fun testDoubleClick(
        filter: PointerFilterScope.() -> Boolean,
        pressButtons: PointerButtons,
    ) = runBlocking {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)
        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->
            var clicksCount = 0
            var doubleClickCount = 0

            scene.setContent {
                Box(
                    modifier = Modifier
                        .combinedClickable(filter = filter, onDoubleClick = {
                            doubleClickCount++
                        }) {
                            clicksCount++
                        }.size(10.dp, 20.dp)
                )
            }

            val upButtons = PointerButtons()
            scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = pressButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
            delay(viewConfiguration.doubleTapTimeoutMillis * 2)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(doubleClickCount).isEqualTo(0)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = pressButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), buttons = upButtons)
            delay(viewConfiguration.doubleTapTimeoutMillis / 2)
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = pressButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), buttons = upButtons)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(doubleClickCount).isEqualTo(1)
        }
    }

    @Test
    fun primaryDoubleClick() = testDoubleClick(
        filter = { button.isPrimary },
        pressButtons = PointerButtons(isPrimaryPressed = true)
    )

    @Test
    fun secondaryDoubleClick() = testDoubleClick(
        filter = { button.isSecondary },
        pressButtons = PointerButtons(isSecondaryPressed = true)
    )

    private fun testLongClick(
        filter: PointerFilterScope.() -> Boolean,
        pressButtons: PointerButtons,
    ) = runBlocking {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)
        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->
            var clicksCount = 0
            var longClickCount = 0

            scene.setContent {
                Box(
                    modifier = Modifier
                        .combinedClickable(filter = filter, onLongPress = {
                            longClickCount++
                        }) {
                            clicksCount++
                        }.size(10.dp, 20.dp)
                )
            }

            val upButtons = PointerButtons(isPrimaryPressed = false)
            scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = pressButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(longClickCount).isEqualTo(0)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = pressButtons)
            delay(viewConfiguration.longPressTimeoutMillis * 2)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(longClickCount).isEqualTo(1)
        }
    }

    @Test
    fun primaryLongClick() = testLongClick(
        filter = { button.isPrimary },
        pressButtons = PointerButtons(isPrimaryPressed = true)
    )

    @Test
    fun secondaryLongClick() = testLongClick(
        filter = { button.isSecondary },
        pressButtons = PointerButtons(isSecondaryPressed = true)
    )

    @Test
    fun `handles primary and secondary clicks`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var primaryClicks = 0
        var secondaryClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .combinedClickable(filter = { button.isPrimary }) {
                        primaryClicks++
                    }
                    .combinedClickable(filter = { button.isSecondary }) {
                        secondaryClicks++
                    }
                    .size(40.dp, 40.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = true)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = false)
        )

        assertThat(primaryClicks).isEqualTo(1)
        assertThat(secondaryClicks).isEqualTo(0)

        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            buttons = PointerButtons(isSecondaryPressed = true)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            buttons = PointerButtons(isSecondaryPressed = false)
        )

        assertThat(primaryClicks).isEqualTo(1)
        assertThat(secondaryClicks).isEqualTo(1)
    }

    @Test
    fun `handles primary click with alt keyModifier`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var genericClicks = 0
        var withAltClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .combinedClickable {
                        genericClicks++
                    }
                    .combinedClickable(
                        filter = { isMouse && button.isPrimary && keyboardModifiers.isAltPressed }
                    ) {
                        withAltClicks++
                    }
                    .size(40.dp, 40.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        // With Alt pressed
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = true),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = false),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true)
        )

        assertThat(withAltClicks).isEqualTo(1)
        assertThat(genericClicks).isEqualTo(0)

        // Without Alt pressed (for generic click handler)
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = true),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = false)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            buttons = PointerButtons(isPrimaryPressed = false),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = false)
        )
        assertThat(withAltClicks).isEqualTo(1)
        assertThat(genericClicks).isEqualTo(1)
    }

    @Test
    fun `consume click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .combinedClickable {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .combinedClickable {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by another click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .combinedClickable {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .combinedClickable {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by pan`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxTotalPan = Offset.Zero
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            outerBoxTotalPan += pan
                        }
                    }
                    .size(80.dp, 80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .combinedClickable {
                            innerBoxClicks++
                        }
                        .size(50.dp, 50.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 0f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), buttons = upButtons)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(20f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), buttons = upButtons)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `draggable by mouse primary button`() {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: (() -> Pair<Offset, PointerKeyboardModifiers>)? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            lateinit var lastDragChange: DragChange

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .draggable(
                            enabled = true,
                            onDragStart = { offset, keyModifiers ->
                                dragStartResult = { offset to keyModifiers }
                            },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                lastDragChange = it
                                onDragCounter++
                            }
                        )
                )
            }

            val downButtons = PointerButtons(isPrimaryPressed = true)
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = downButtons)
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f + viewConfiguration.touchSlop, 5f), buttons = downButtons)

            assertEquals(
                Offset(5f, 5f) to PointerKeyboardModifiers(),
                dragStartResult?.invoke()
            )
            assertEquals(ceil(viewConfiguration.touchSlop), ceil(lastDragChange.offset.x))
            assertEquals(1, onDragCounter)
            assertEquals(0f, 0f)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f + viewConfiguration.touchSlop, 15f), buttons = downButtons)
            assertEquals(0f, lastDragChange.offset.x)
            assertEquals(10f, lastDragChange.offset.y)
            assertEquals(2, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f + viewConfiguration.touchSlop, 25f), buttons = downButtons)
            assertEquals(0f, lastDragChange.offset.x)
            assertEquals(10f, lastDragChange.offset.y)
            assertEquals(3, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Release, Offset(viewConfiguration.touchSlop, 15f))
            assertEquals(-5f, lastDragChange.offset.x)
            assertEquals(-10f, lastDragChange.offset.y)
            assertTrue(dragEnded)
            assertFalse(dragCanceled)
            assertEquals(4, onDragCounter)
        }
    }
}
