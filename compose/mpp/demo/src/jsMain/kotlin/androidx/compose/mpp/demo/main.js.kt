package androidx.compose.mpp.demo

import androidx.compose.ui.window.JsCanvasTypefaces
import androidx.compose.ui.window.Window
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.skiko.GenericSkikoView
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.loadBytesFromPath
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLCanvasElement

fun main() {

    MainScope().launch {
        JsCanvasTypefaces.mapAliasToPath = mapOf(
            "Noto Color Emoji" to loadBytesFromPath("NotoColorEmoji.ttf"),
            "Noto Sans" to loadBytesFromPath("NotoSans-Regular.ttf"),
            "Noto Serif" to loadBytesFromPath("NotoSerif-Regular.ttf")
        )

        onWasmReady {
            Window(
                title = "Compose/JS sample"
            ) {
                myContent()
            }
        }
    }
}
