import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.js.ComposeWindow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import androidx.compose.foundation.background
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    val red = mutableStateOf(false)

    onWasmReady {
        ComposeWindow().setContent {
            println("setContent lambda is running")
            Column {
                Box(modifier = Modifier.width(100.dp).height(100.dp)
                    .background(if (red.value) Color.Red else Color.Blue)
                )
            }
        }
    }

    document.getElementById(elementId = "checkRed")!!.addEventListener("change", {
        red.value = !red.value
    })
}