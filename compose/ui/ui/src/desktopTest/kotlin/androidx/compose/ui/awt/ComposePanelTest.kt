package androidx.compose.ui.awt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Test
import java.awt.Dimension
import javax.swing.JFrame

class ComposePanelTest {
    @Test
    fun `don't override user preferred size`() {
        runBlocking(Dispatchers.Swing) {
            val composePanel = ComposePanel()
            composePanel.preferredSize = Dimension(234, 345)
            assertThat(composePanel.preferredSize).isEqualTo(Dimension(234, 345))

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                frame.isVisible = true
                assertThat(composePanel.preferredSize).isEqualTo(Dimension(234, 345))

                frame.pack()
                assertThat(composePanel.size).isEqualTo(Dimension(234, 345))
                assertThat(frame.size).isEqualTo(Dimension(234, 345))
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `pack to Compose content`() {
        runBlocking(Dispatchers.Swing) {
            val composePanel = ComposePanel()
            composePanel.setContent {
                Box(Modifier.requiredSize(300.dp, 400.dp))
            }

            val frame = JFrame()
            try {
                frame.contentPane.add(composePanel)
                frame.isUndecorated = true

                frame.pack()
                assertThat(composePanel.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(frame.preferredSize).isEqualTo(Dimension(300, 400))

                frame.isVisible = true
                assertThat(composePanel.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(frame.preferredSize).isEqualTo(Dimension(300, 400))
            } finally {
                frame.dispose()
            }
        }
    }
}