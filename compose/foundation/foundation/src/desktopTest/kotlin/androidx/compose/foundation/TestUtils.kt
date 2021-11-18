package androidx.compose.foundation

import java.awt.Component
import java.awt.event.MouseWheelEvent

private val EventComponent = object : Component() {}

internal fun awtWheelEvent(isScrollByPages: Boolean = false) = MouseWheelEvent(
    EventComponent,
    MouseWheelEvent.MOUSE_WHEEL,
    0,
    0,
    0,
    0,
    0,
    false,
    if (isScrollByPages) {
        MouseWheelEvent.WHEEL_BLOCK_SCROLL
    } else {
        MouseWheelEvent.WHEEL_UNIT_SCROLL
    },
    1,
    0
)