package androidx.compose.native.demo

import platform.AppKit.*

import androidx.compose.native.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.ColumnScope
//import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Switch
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.Slider
import androidx.compose.material.Snackbar
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Constraints.*

fun main() {
    NSApplication.sharedApplication()
    createWindow("Compose/Native sample")
    NSApp?.run()
}

fun createWindow(title: String) {
    println("createWindow()")
    /*val window = */
    Window(title) {
//        Box(modifier = Modifier
//            .padding(16.dp)
//            .background(color = Color.Red)
//            .width(100.dp).height(100.dp)
//        ) {
//            Box(modifier = Modifier
//                .padding(16.dp)
//                .background(color = Color.Blue)
//                .width(10.dp).height(10.dp)
//            )
//        }

        //var text by remember { mutableStateOf("Hello, World!") }

//        CoreText(AnnotatedString("HEEHE"))
//        BasicText("HELLO")
        //Text("Hello from Native")
//        Button(onClick = {}) {
//        }

//        Checkbox(
//            checked = false,
//            onCheckedChange = {  }
//        )

//        Card(backgroundColor = Color.White) {
//            Box(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .background(color = Color.Red)
//                    .width(100.dp).height(100.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .background(color = Color.Blue)
//                        .width(10.dp).height(10.dp)
//                )
//            }
//        }
        //Text("adasdasdasdasd", color = Color.White)

//        Switch(true, null)
        Snackbar {}
//        Slider(1f, {})

//        Spacer(Modifier.width(100.dp).height(100.dp))
        //*/
    }
    println("end createWindow()")
}


@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) =
    Spacer(modifier.drawBehind(onDraw))

@Composable
fun Spacer2(modifier: Modifier) {
    val mp = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            println("MeasureResultMeasureResultMeasureResultMeasureResult")
            return object : MeasureResult {
                override val width = 100
                override val height = 100
                override val alignmentLines = emptyMap<AlignmentLine, Int>()
                override fun placeChildren() {
                    println("PLACE CHILDER")
//                    Placeable.PlacementScope.executeWithRtlMirroringValues(
//                        width,
//                        layoutDirection,
//                        placementBlock
//                    )
                }
            }
        }
    }
    Layout({}, modifier, mp)
//    { _, constraints ->
//        with(constraints) {
//            val width = if (hasFixedWidth) maxWidth else 0
//            val height = if (hasFixedHeight) maxHeight else 0
//            println("MEASSSSUREEE")
//            object : MeasureResult {
//                override val width = width
//                override val height = height
//                override val alignmentLines = emptyMap<AlignmentLine, Int>()
//                override fun placeChildren() {
////                    Placeable.PlacementScope.executeWithRtlMirroringValues(
////                        width,
////                        layoutDirection,
////                        placementBlock
////                    )
//                }
//                //layout(width, height) {}
//            }
//        }
//    }
}