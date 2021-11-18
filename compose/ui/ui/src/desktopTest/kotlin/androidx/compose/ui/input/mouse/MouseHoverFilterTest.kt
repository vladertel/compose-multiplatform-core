/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.awtWheelEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class MouseHoverFilterTest {
    @Test
    fun `inside window`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 20f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 15f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(2)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(2)
    }

    @Test
    fun `window enter`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(0)
    }

    // TODO(demin): fix race condition with GlobalSnapshotManager. without it test is flaky.
    private fun useImageComposeSceneInSwingThread(
        width: Int,
        height: Int,
        density: Density = Density(1f),
        body: (ImageComposeScene) -> Unit
    ) {
        runBlocking(Dispatchers.Swing) {
            ImageComposeScene(
                width = width,
                height = height,
                density = density
            ).use(body)
        }
    }

    @Test
    fun `scroll should trigger enter and exit`() = useImageComposeSceneInSwingThread(
        width = 100,
        height = 100,
        density = Density(2f)
    ) { scene ->
        val boxCount = 3

        val enterCounts = Array(boxCount) { 0 }
        val exitCounts = Array(boxCount) { 0 }

        scene.setContent {
            Column(
                Modifier
                    .size(10.dp, 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(boxCount) { index ->
                    Box(
                        modifier = Modifier
                            .pointerMove(
                                onMove = {},
                                onEnter = {
                                    enterCounts[index] = enterCounts[index] + 1
                                },
                                onExit = {
                                    exitCounts[index] = exitCounts[index] + 1
                                }
                            )
                            .size(10.dp, 20.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(
            eventType = PointerEventType.Enter,
            position = Offset.Zero,
        )

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, 1f),
            nativeEvent = awtWheelEvent(isScrollByPages = true),
        )
        scene.render() // synthetic enter/exit will trigger only on relayout
        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 0))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 0, 0))

        scene.sendPointerEvent(
            eventType = PointerEventType.Move,
            position = Offset(1f, 1f),
        )
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(2f, 2f),
            scrollDelta = Offset(0f, 1f),
            nativeEvent = awtWheelEvent(isScrollByPages = true),
        )
        scene.render()

        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 1))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 1, 0))

        scene.sendPointerEvent(
            eventType = PointerEventType.Exit,
            position = Offset(-1f, -1f),
        )
        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 1))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 1, 1))
    }
}

private fun Modifier.pointerMove(
    onMove: () -> Unit,
    onExit: () -> Unit,
    onEnter: () -> Unit,
): Modifier = pointerInput(onMove, onExit, onEnter) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Move -> onMove()
                PointerEventType.Enter -> onEnter()
                PointerEventType.Exit -> onExit()
            }
        }
    }
}
