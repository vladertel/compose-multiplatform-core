package androidx.compose.mpp.demo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("Compose/JS sample", canvasElementId = "canvas1") {
            val app = remember { App() }
            app.Content()
        }
    }
}
