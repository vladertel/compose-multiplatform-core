package androidx.compose.mpp.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.SkiaLayer
import org.w3c.dom.HTMLCanvasElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("Compose/JS sample", canvasElementId = "canvas1") {
        val app = remember { App() }
        app.Content()
    }
}
