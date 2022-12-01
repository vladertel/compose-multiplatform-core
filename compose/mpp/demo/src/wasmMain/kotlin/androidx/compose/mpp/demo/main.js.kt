package androidx.compose.mpp.demo

import androidx.compose.ui.window.Window
import kotlinx.browser.document
import org.jetbrains.skiko.GenericSkikoView
import org.jetbrains.skiko.SkiaLayer
import org.w3c.dom.HTMLCanvasElement

fun main() {
    Window("Compose/JS sample") {
        myContent()
    }
}
