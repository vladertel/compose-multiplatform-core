package androidx.compose.native.demo

import platform.AppKit.*

import androidx.compose.native.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/*
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
*/

fun main() {
    NSApplication.sharedApplication()
    createWindow("Compose/Native sample")
    NSApp?.run()
}

fun createWindow(title: String) {
    println("createWindow()")
    /*val window = */
    Window(title) {
        Box(modifier = Modifier
            .padding(16.dp)
            .background(color = Color.Red)
            .width(100.dp).height(100.dp)
        ) {
            Box(modifier = Modifier
                .padding(16.dp)
                .background(color = Color.Blue)
                .width(10.dp).height(10.dp)
            )
        }
        /*
        var text by remember { mutableStateOf("Hello, World!") }

        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }

        */
    }
    println("end createWindow()")
}
