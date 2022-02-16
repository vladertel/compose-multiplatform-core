/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import org.jetbrains.skiko.SkikoPointerEvent

internal actual typealias NativePointerButtons = Int
internal actual typealias NativePointerKeyboardModifiers = Int

/**
 * Describes a pointer input change event that has occurred at a particular point in time.
 */
actual data class PointerEvent internal constructor(
    /**
     * The changes.
     */
    actual val changes: List<PointerInputChange>,

    /**
     * Original raw native event from AWT
     */
    val mouseEvent: SkikoPointerEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(changes, internalPointerEvent?.mouseEvent)

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(changes, mouseEvent = null)

    actual var type: PointerEventType = PointerEventType.Unknown
        internal set

    actual val buttons: PointerButtons = PointerButtons(mouseEvent?.button?.value ?: 0)

    actual val keyboardModifiers: PointerKeyboardModifiers
        get() = TODO("implement native pointer event")

}


actual val PointerButtons.isPrimaryPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = TODO("implement native events")


actual val PointerButtons.isBackPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isForwardPressed: Boolean
    get() = TODO("implement native events")

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    TODO("implement native events")

// TODO: all this file should go away when we move to Skiko events for skikoCommon.
actual val PointerButtons.areAnyPressed: Boolean
    get() = packedValue != 0

actual fun PointerButtons.indexOfFirstPressed(): Int = TODO("implement native events")

actual fun PointerButtons.indexOfLastPressed(): Int = TODO("implement native events")

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = TODO("implement native events")

/**
 * Creates [PointerButtons] with the specified state of the pressed buttons.
 */
fun PointerButtons(
    isPrimaryPressed: Boolean = false,
    isSecondaryPressed: Boolean = false,
    isTertiaryPressed: Boolean = false,
    isBackPressed: Boolean = false,
    isForwardPressed: Boolean = false
): PointerButtons {
    var res = 0
    if (isPrimaryPressed) res = res or ButtonMasks.Primary
    if (isSecondaryPressed) res = res or ButtonMasks.Secondary
    if (isTertiaryPressed) res = res or ButtonMasks.Tertiary
    if (isBackPressed) res = res or ButtonMasks.Back
    if (isForwardPressed) res = res or ButtonMasks.Forward
    return PointerButtons(res)
}

private object ButtonMasks {
    const val Primary = 1 shl 0
    const val Secondary = 1 shl 1
    const val Tertiary = 1 shl 2
    const val Back = 1 shl 3
    const val Forward = 1 shl 4
}

private object KeyboardModifierMasks {
    const val CtrlPressed = 1 shl 0
    const val MetaPressed = 1 shl 1
    const val AltPressed = 1 shl 2
    const val AltGraphPressed = 1 shl 3
    const val SymPressed = 1 shl 4
    const val ShiftPressed = 1 shl 5
    const val FunctionPressed = 1 shl 6
    const val CapsLockOn = 1 shl 7
    const val ScrollLockOn = 1 shl 8
    const val NumLockOn = 1 shl 9
}

/**
 * Creates [PointerKeyboardModifiers] with the specified state of the pressed keyboard modifiers.
 */
fun PointerKeyboardModifiers(
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isAltPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    isAltGraphPressed: Boolean = false,
    isSymPressed: Boolean = false,
    isFunctionPressed: Boolean = false,
    isCapsLockOn: Boolean = false,
    isScrollLockOn: Boolean = false,
    isNumLockOn: Boolean = false,
): PointerKeyboardModifiers {
    var res = 0
    if (isCtrlPressed) res = res or KeyboardModifierMasks.CtrlPressed
    if (isMetaPressed) res = res or KeyboardModifierMasks.MetaPressed
    if (isAltPressed) res = res or KeyboardModifierMasks.AltPressed
    if (isShiftPressed) res = res or KeyboardModifierMasks.ShiftPressed
    if (isAltGraphPressed) res = res or KeyboardModifierMasks.AltGraphPressed
    if (isSymPressed) res = res or KeyboardModifierMasks.SymPressed
    if (isFunctionPressed) res = res or KeyboardModifierMasks.FunctionPressed
    if (isCapsLockOn) res = res or KeyboardModifierMasks.CapsLockOn
    if (isScrollLockOn) res = res or KeyboardModifierMasks.ScrollLockOn
    if (isNumLockOn) res = res or KeyboardModifierMasks.NumLockOn
    return PointerKeyboardModifiers(res)
}


