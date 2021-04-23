/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.examples.windowapi

import androidx.compose.desktop.ComposeWindow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.lang.Thread.currentThread
import javax.imageio.ImageIO

fun helloWorld() = GlobalScope.launchApplication {
    Window {
        Text("Hello, World!")
    }
}

fun suspendApplication() = GlobalScope.launch {
    println("Before application")

    awaitApplication {
        Window {}
    }

    println("After application")
}

fun suspendBackgroundApplication() = GlobalScope.launch {
    println("Before application")

    awaitApplication {
        LaunchedEffect(Unit) {
            println("1")
            delay(1000)
            println("2")
            delay(1000)
            println("3")
        }
    }

    println("After application")
}

fun splashScreen() = GlobalScope.launchApplication {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    if (isLoading) {
        Window {
            Text("Loading")
        }
    } else {
        Window {
            Text("Hello, World!")
        }
    }
}

fun autoClose() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isShowing = false
    }

    if (isShowing) {
        Window {
            Text("This window will be closed in 2 seconds")
        }
    }
}

fun changingOpenState() = GlobalScope.launchApplication {
    val state = rememberWindowState(isOpen = false)

    Window {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(state.isOpen, { state.isOpen = !state.isOpen })
            Text("Nested window")
        }
    }

    Window(state, title = "Nested window") {}
}

fun closeToTray() = GlobalScope.launchApplication {
    val state = rememberWindowState(isVisible = false)

    Window(
        state,
        title = "Counter",
        onCloseRequest = { state.isVisible = false }
    ) {
        var counter by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                counter++
                delay(1000)
            }
        }
        Text(counter.toString())
    }

    val icon = remember {
        runBlocking {
            loadIcon()
        }
    }

    if (!state.isVisible && state.isOpen) {
        Tray(
            icon,
            hint = "Counter",
            onAction = { state.isVisible = true },
            menu = {
                Item("Exit", onClick = { state.isOpen = false })
            },
        )
    }
}

fun askToClose() = GlobalScope.launchApplication {
    val mainWindowState = rememberWindowState()
    val askToCloseWindowState = rememberWindowState(isOpen = false)

    Window(mainWindowState, onCloseRequest = { askToCloseWindowState.isOpen = true }) {
        Text("Very important document")

        Window(askToCloseWindowState, title = "Are you sure?") {
            Button(onClick = { mainWindowState.isOpen = false }) {
                Text("Yes!")
            }
        }
    }
}

fun nestedWindow() = GlobalScope.launchApplication {
    Window(title = "nestedWindow") {
        VeryComplexScreen()
    }
}

@Composable
private fun VeryComplexScreen() = Column {
    var isNestedWindowShowing by remember { mutableStateOf(true) }

    Text("Hello, World!")
    Button(
        onClick = {
            isNestedWindowShowing = !isNestedWindowShowing
        }
    ) {
        Text("Nested window")
    }

    if (isNestedWindowShowing) {
        Window(onCloseRequest = { isNestedWindowShowing = false }) {
            Text("Nested window")
        }
    }
}

fun customWindow() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }
    var titleNum by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            titleNum++
            delay(1000)
        }
    }

    if (isShowing) {
        Window(
            create = {
                ComposeWindow().apply {
                    size = Dimension(200, 200)
                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent) {
                            isShowing = false
                        }
                    })
                }
            },
            dispose = ComposeWindow::dispose,
            update = {
                it.title = "title$titleNum"
            }
        ) {}
    }
}

fun dialog() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }
    var isDialogShowing by remember { mutableStateOf(false) }

    if (isShowing) {
        Window(onCloseRequest = { isShowing = false }) {
            Button(onClick = { isDialogShowing = true }) {
                Text("Dialog")
            }

            if (isDialogShowing) {
                Dialog(onCloseRequest = { isDialogShowing = false }) {
                    Text("Dialog")
                }
            }
        }
    }
}

fun hideDialog() = GlobalScope.launchApplication {
    val dialogState = rememberDialogState()

    Window {
        Button(onClick = { dialogState.isVisible = true }) {
            Text("Dialog")
        }

        Dialog(
            state = dialogState,
            onCloseRequest = { dialogState.isVisible = false }
        ) {
            var counter by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    counter++
                    delay(1000)
                }
            }
            Text(counter.toString())
        }
    }
}

fun customDialog() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }

    if (isShowing) {
        FileDialog(
            onDismissRequest = {
                isShowing = false
                println("Result $it")
            }
        )
    }
}

@Composable
private fun FileDialog(
    onDismissRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(null as Frame?, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onDismissRequest(file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

fun setIcon() = GlobalScope.launchApplication {
    var icon: BufferedImage? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        delay(1000)
        icon = loadIcon()
        delay(1000)
        icon = null
        delay(1000)
        icon = loadIcon()
    }

    Window(icon = icon) {}
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun loadIcon() = withContext(Dispatchers.IO) {
    val path = "androidx/compose/desktop/example/tray.png"
    ImageIO.read(currentThread().contextClassLoader.getResource(path))
}

fun setParameters() = GlobalScope.launchApplication {
    val state = rememberWindowState()
    Window(state, resizable = false, undecorated = true, alwaysOnTop = true) {
        Button(onClick = { state.isOpen = false }) {
            Text("Close")
        }
    }
}

fun setPosition() = GlobalScope.launchApplication {
    val state = rememberWindowState(position = WindowPosition(0.dp, 0.dp))

    Window(state) {}

    Window {
        Column {
            Text(state.position.toString())

            Button(
                onClick = {
                    state.position = state.position.copy(x = state.position.x + 10.dp)
                }
            ) {
                Text("move")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isOpen, { state.isOpen = !state.isOpen })
                Text("isOpen")
            }
        }
    }
}

fun initiallyCenteredWindow() = GlobalScope.launchApplication {
    val state = rememberWindowState()

    Window(state, initialAlignment = Alignment.Center) {}

    Window {
        Column {
            Text(state.position.toString())

            Button(
                onClick = {
                    state.position = state.position.copy(x = state.position.x + 10.dp)
                }
            ) {
                Text("move")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isOpen, { state.isOpen = !state.isOpen })
                Text("isOpen")
            }
        }
    }
}

fun setSize() = GlobalScope.launchApplication {
    val state = rememberWindowState(size = WindowSize(400.dp, 100.dp))

    Window(state) {}

    Window {
        Column {
            Text(state.size.toString())

            Button(
                onClick = {
                    state.size = state.size.copy(width = state.size.width + 10.dp)
                }
            ) {
                Text("resize")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isOpen, { state.isOpen = !state.isOpen })
                Text("isOpen")
            }
        }
    }
}

fun setStatus() = GlobalScope.launchApplication {
    val state = rememberWindowState(isMaximized = true, isMinimized = false)

    Window(state) {
        Column {
            Text(state.size.toString())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isFullscreen, { state.isFullscreen = !state.isFullscreen })
                Text("isFullscreen")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isMaximized, { state.isMaximized = !state.isMaximized })
                Text("isMaximized")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isMinimized, { state.isMinimized = !state.isMinimized })
                Text("isMinimized")
            }
        }
    }
}

fun hotKeys() = GlobalScope.launchApplication {
    val state = rememberWindowState()

    Window(state) {
        val focusRequester = remember(::FocusRequester)
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            Modifier
                .focusRequester(focusRequester)
                .focusModifier()
                .onPreviewKeyEvent {
                    when (it.key) {
                        Key.Escape -> {
                            state.isOpen = false
                            true
                        }
                        else -> false
                    }
                }
        ) {
            TextField("Text", {})
        }
    }
}

fun saveWindowState() {
    // TODO
}

fun menu() = GlobalScope.launchApplication {
    val state = rememberWindowState()
    var isSubmenuShowing by remember { mutableStateOf(false) }

    Window(
        state
    ) {
        MenuBar {
            Menu("File") {
                Item(
                    "Toggle submenu",
                    onClick = {
                        isSubmenuShowing = !isSubmenuShowing
                    }
                )
                if (isSubmenuShowing) {
                    Menu("Submenu") {
                        Item(
                            "item1",
                            onClick = {
                                println("item1")
                            }
                        )
                        Item(
                            "item2",
                            onClick = {
                                println("item2")
                            }
                        )
                    }
                }
                Separator()
                Item("Exit", onClick = { state.isOpen = false })
            }
        }
    }
}

fun trayAndNotifications() = GlobalScope.launchApplication {
    val state = remember(::AppState)

    Window(state.window) {
        TrayScreen(state)
    }
}

private class AppState {
    val window = WindowState()
    val tray = TrayState()
    var isTray2Visible by mutableStateOf(false)
}

@Composable
private fun TrayScreen(state: AppState) = Column(Modifier.padding(12.dp)) {
    val notification = Notification(
        title = "Title",
        message = "Text",
        Notification.Type.Info
    )

    Button(
        modifier = Modifier.padding(8.dp),
        onClick = {
            state.isTray2Visible = !state.isTray2Visible
        }
    ) {
        Text("Toggle middle tray")
    }

    Button(
        modifier = Modifier.padding(8.dp),
        onClick = {
            state.tray.sendNotification(notification)
        }
    ) {
        Text("Show notification")
    }

    Trays(state)
}

@Composable
private fun Trays(state: AppState) {
    val icon = remember {
        runBlocking {
            loadIcon()
        }
    }

    Tray(icon, hint = "Tray1")

    if (state.isTray2Visible) {
        Tray(
            icon = icon,
            state = state.tray,
            hint = "Tray2",
            menu = {
                Menu("Submenu") {
                    Item(
                        "item1",
                        onClick = {
                            println("item1")
                        }
                    )
                    Item(
                        "item2",
                        onClick = {
                            println("item2")
                        }
                    )
                }

                Separator()

                Item(
                    "Exit",
                    onClick = { state.window.isOpen = false }
                )
            }
        )
    }

    Tray(icon)
}
