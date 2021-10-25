package androidx.compose.ui.awt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Assume
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.awt.Dimension

class ComposeWindowTest {
    @Test
    fun `don't override user preferred size`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(Dispatchers.Swing) {
            val window = ComposeWindow()
            try {
                window.preferredSize = Dimension(234, 345)
                window.isUndecorated = true
                window.isVisible = true
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

        runBlocking(Dispatchers.Swing) {
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
}