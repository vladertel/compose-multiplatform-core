package androidx.compose.native.demo

import platform.AppKit.*

import androidx.compose.native.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.ui.*
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

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
    Window(title) {
        Column {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(color = Color.Red)
                    .width(100.dp).height(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(color = Color.Blue)
                        .width(10.dp).height(10.dp)
                )
            }

            Spacer(
                Modifier.width(200.dp).height(4.dp).background(color = Color.DarkGray)
            )

            Button(
                modifier = Modifier
                    .padding(16.dp),
                onClick = {
                    println("HELLO again!")
                }
            ) {
            }
        }
    }
    println("end createWindow()")
}

