package androidx.compose.ui.awt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Assume
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.awt.Dimension
import java.awt.event.MouseEvent

class ComposeWindowTest {
    @Test
    fun `don't override user preferred size`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val window = ComposeWindow()
            try {
                window.preferredSize = Dimension(234, 345)
                window.isUndecorated = true
                assertThat(window.preferredSize).isEqualTo(Dimension(234, 345))
                window.pack()
                assertThat(window.size).isEqualTo(Dimension(234, 345))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `pack to Compose content`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(MainUIDispatcher) {
            val window = ComposeWindow()
            try {
                window.setContent {
                    Box(Modifier.requiredSize(300.dp, 400.dp))
                }
                window.isUndecorated = true

                window.pack()
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))

                window.isVisible = true
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `a single layout pass at the window start`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val layoutPassConstraints = mutableListOf<Constraints>()

        runBlocking(MainUIDispatcher) {
            val window = ComposeWindow()
            try {
                window.size = Dimension(300, 400)
                window.setContent {
                    Box(Modifier.fillMaxSize().layout { _, constraints ->
                        layoutPassConstraints.add(constraints)
                        layout(0, 0) {}
                    })
                }

                window.isUndecorated = true
                window.isVisible = true
                window.paint(window.graphics)
                assertThat(layoutPassConstraints).isEqualTo(
                    listOf(
                        Constraints.fixed(
                            width = (300 * window.density.density).toInt(),
                            height = (400 * window.density.density).toInt(),
                        )
                    )
                )
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `dispose window in event handler`() = runApplicationTest {
        val window = ComposeWindow()
        try {
            window.size = Dimension(300, 400)
            window.setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                    window.dispose()
                })
            }
            window.isVisible = true
            window.sendMouseEvent(MouseEvent.MOUSE_ENTERED, x = 100, y = 50)
            awaitIdle()
            window.sendMouseEvent(MouseEvent.MOUSE_MOVED, x = 100, y = 50)
            awaitIdle()
            window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 100, y = 50, modifiers = MouseEvent.BUTTON1_DOWN_MASK)
            awaitIdle()
            window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 100, y = 50)
            awaitIdle()
        } finally {
            window.dispose()
        }
    }
}