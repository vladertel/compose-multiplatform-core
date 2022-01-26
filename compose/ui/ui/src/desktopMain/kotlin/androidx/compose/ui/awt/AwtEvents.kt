package androidx.compose.ui.awt

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent


// TODO(demin): new API, which is not merged to AOSP
/**
 * The original raw native event from AWT
 */
val PointerEvent.awtEvent: java.awt.event.MouseEvent get() {
    require(nativeEvent is java.awt.event.MouseEvent) {
        "nativeEvent was sent not by AWT. Make sure, that you use AWT backed API" +
                " (from androidx.compose.ui.awt.* or from androidx.compose.ui.window.*)"
    }
    return nativeEvent
}

// TODO(demin): new API, which is not merged to AOSP
/**
 * The original raw native event from AWT
 */
val KeyEvent.awtEvent: java.awt.event.KeyEvent get() {
    require(nativeKeyEvent is java.awt.event.KeyEvent) {
        "nativeKeyEvent was sent not by AWT. Make sure, that you use AWT backed API" +
                " (from androidx.compose.ui.awt.* or from androidx.compose.ui.window.*)"
    }
    return nativeKeyEvent
}