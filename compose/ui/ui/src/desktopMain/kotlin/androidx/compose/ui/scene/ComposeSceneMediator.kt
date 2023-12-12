/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.input.pointer.AwtCursor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.AccessibilityController
import androidx.compose.ui.platform.ComposeSceneAccessible
import androidx.compose.ui.platform.DelegateRootForTestListener
import androidx.compose.ui.platform.DesktopTextInputService
import androidx.compose.ui.platform.EmptyViewConfiguration
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.scaledSize
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.im.InputMethodRequests
import javax.accessibility.Accessible
import javax.swing.JLayeredPane
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.ClipComponent
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.hostOs

internal class ComposeSceneMediator(
    private val container: JLayeredPane,
    private val windowContext: PlatformWindowContext,
    private var exceptionHandler: WindowExceptionHandler?,

    val coroutineContext: CoroutineContext,

    skiaLayerComponentFactory: (ComposeSceneMediator) -> SkiaLayerComponent,
    composeSceneFactory: (ComposeSceneMediator) -> ComposeScene,
) : ContainerListener, InputMethodListener, FocusListener, MouseListener, MouseMotionListener,
    MouseWheelListener, KeyListener {
    private var isDisposed = false
    private val invisibleComponent = InvisibleComponent()

    val skikoView: SkikoView = DesktopSkikoView()

    private val clipComponents = mutableMapOf<Component, ClipRectangle>()
    private var onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false }
    private var onKeyEvent: (ComposeKeyEvent) -> Boolean = { false }

    private val semanticsOwnerListener = DesktopSemanticsOwnerListener()
    var rootForTestListener: PlatformContext.RootForTestListener? by DelegateRootForTestListener()

    private val platformComponent = DesktopPlatformComponent()
    private val textInputService = DesktopTextInputService(platformComponent)
    private val _platformContext = DesktopPlatformContext()
    val platformContext: PlatformContext get() = _platformContext

    private val skiaLayerComponent by lazy { skiaLayerComponentFactory(this) }
    private val clipRectangles by skiaLayerComponent::clipComponents
    val contentComponent by skiaLayerComponent::contentComponent
    var transparency: Boolean
        get() = skiaLayerComponent.transparency
        set(value) {
            skiaLayerComponent.transparency = value || useInteropBlending
        }
    var fullscreen by skiaLayerComponent::fullscreen
    val windowHandle by skiaLayerComponent::windowHandle
    val renderApi by skiaLayerComponent::renderApi

    var contentBounds: Rectangle
        get() = contentComponent.bounds
        set(value) {
            contentComponent.bounds = value
        }
    private val scaledOffset: Offset
        get() {
            val scale = scene.density.density
            return Offset(
                x = contentBounds.x * scale,
                y = contentBounds.y * scale
            )
        }

    var overrideOffset: Offset? = null
    var overrideSize: IntSize? = null
        set(value) {
            field = value
            onChangeComponentSize()
        }

    private val scene by lazy { composeSceneFactory(this) }
    val focusManager get() = scene.focusManager
    var compositionLocalContext: CompositionLocalContext?
        get() = scene.compositionLocalContext
        set(value) { scene.compositionLocalContext = value }
    val accessible: Accessible = ComposeSceneAccessible {
        semanticsOwnerListener.accessibilityControllers
    }

    var currentInputMethodRequests: InputMethodRequests? = null
        private set

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events.
     * This flag is set when window focus changes. Then we can rely on it when handling the
     * first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used
     * to update windowInfo.keyboardModifiers.
     *
     * TODO: needs to be set `true` when focus changes:
     * (Window focus change is implemented in JB fork, but not upstreamed yet).
     */
    private var keyboardModifiersRequireUpdate = false

    /**
     * Provides the size of ComposeScene content inside infinity constraints
     *
     * This is needed for the bridge between Compose and Swing since
     * in some cases, Swing's LayoutManagers need
     * to calculate the preferred size of the content without max/min constraints
     * to properly lay it out.
     *
     * Example: Compose content inside Popup without a preferred size.
     * Swing will calculate the preferred size of the Compose content and set Popup's side for that.
     *
     * See [androidx.compose.ui.awt.ComposePanelTest] test `initial panel size of LazyColumn with border layout`
     */
    val preferredSize: Dimension
        get() {
            val contentSize = scene.calculateContentSize()
            val scale = scene.density.density
            return Dimension(
                (contentSize.width / scale).toInt(),
                (contentSize.height / scale).toInt()
            )
        }

    private val useInteropBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending && skiaLayerComponent.interopBlendingSupported

    private val bridgeLayer: Int = 10
    private val componentLayer: Int
        get() = if (useInteropBlending) 0 else 20

    init {
        container.addToLayer(invisibleComponent, bridgeLayer)
        container.addToLayer(contentComponent, bridgeLayer)
        container.addContainerListener(this)

        // It will be enabled dynamically. See DesktopPlatformComponent
        contentComponent.enableInputMethods(false)
        contentComponent.focusTraversalKeysEnabled = false

        subscribe(contentComponent)

        skiaLayerComponent.transparency = useInteropBlending
    }

    fun dispose() {
        check(!isDisposed) { "ComposeSceneMediator is already disposed" }
        isDisposed = true

        unsubscribe(contentComponent)

        container.removeContainerListener(this)
        container.remove(contentComponent)
        container.remove(invisibleComponent)

        scene.close()
        skiaLayerComponent.dispose()
    }

    private inline fun catchExceptions(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            exceptionHandler?.onException(e) ?: throw e
        }
    }

    private fun subscribe(component: Component) {
        component.addInputMethodListener(this)
        component.addFocusListener(this)
        component.addMouseListener(this)
        component.addMouseMotionListener(this)
        component.addMouseWheelListener(this)
        component.addKeyListener(this)
    }

    private fun unsubscribe(component: Component) {
        component.removeInputMethodListener(this)
        component.removeFocusListener(this)
        component.removeMouseListener(this)
        component.removeMouseMotionListener(this)
        component.removeMouseWheelListener(this)
        component.removeKeyListener(this)
    }

    fun onChangeComponentSize() {
        if (!container.isDisplayable) return

        // Zero size will literally limit scene's content size to zero,
        // so it case of late initialization skip this to avoid extra layout run.
        val scaledSize = overrideSize ?: container.scaledSize.takeIf { it != IntSize.Zero }
        if (scene.size != scaledSize) {
            scene.size = scaledSize
        }
    }

    fun onChangeComponentDensity() {
        if (!container.isDisplayable) return
        val density = container.density
        if (scene.density != density) {
            scene.density = density
            onChangeComponentSize()
        }
    }

    fun onChangeLayoutDirection(layoutDirection: LayoutDirection) {
        scene.layoutDirection = layoutDirection
    }

    fun onRenderApiChanged(action: () -> Unit) {
        skiaLayerComponent.onRenderApiChanged(action)
    }

    fun onChangeWindowFocus() {
        keyboardModifiersRequireUpdate = true
    }

    fun onComponentAttached() {
        onChangeComponentDensity()

        _onComponentAttached?.invoke()
        _onComponentAttached = null
    }

    private fun onMouseEvent(event: MouseEvent): Unit = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) return
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            windowContext.setKeyboardModifiers(event.keyboardModifiers)
        }
        val density = contentComponent.density
        val scaledOffset = overrideOffset ?: scaledOffset
        scene.onMouseEvent(scaledOffset, density, event)
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent): Unit = catchExceptions {
        if (isDisposed) return
        val density = contentComponent.density
        val scaledOffset = overrideOffset ?: scaledOffset
        scene.onMouseWheelEvent(scaledOffset, density, event)
    }

    private fun onKeyEvent(event: KeyEvent) = catchExceptions {
        if (isDisposed) return
        textInputService.onKeyEvent(event)
        windowContext.setKeyboardModifiers(event.toPointerKeyboardModifiers())

        val composeEvent = ComposeKeyEvent(event)
        if (onPreviewKeyEvent(composeEvent) ||
            scene.sendKeyEvent(composeEvent) ||
            onKeyEvent(composeEvent)
        ) {
            event.consume()
        }
    }

    private fun JLayeredPane.addToLayer(component: Component, layer: Int) {
        if (renderApi == GraphicsApi.METAL) {
            // Applying layer on macOS makes our bridge non-transparent
            // But it draws always on top, so we can just add it as-is
            // TODO: Figure out why it makes difference in transparency
            add(component, 0)
        } else {
            setLayer(component, layer)
            add(component, null, -1)
        }
    }

    fun addToComponentLayer(component: Component) {
        container.addToLayer(component, componentLayer)
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
    ) {
        this.onPreviewKeyEvent = onPreviewKeyEvent
        this.onKeyEvent = onKeyEvent
    }

    private var _onComponentAttached: (() -> Unit)? = null
    private fun runOnceComponentAttached(block: () -> Unit) {
        if (contentComponent.isDisplayable) {
            block()
        } else {
            _onComponentAttached = block
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        runOnceComponentAttached {
            catchExceptions {
                scene.setContent(content)
            }
        }
    }

    fun onComposeSceneInvalidate() {
        if (isDisposed) return
        skiaLayerComponent.onComposeSceneInvalidate()
    }

    private fun resetFocus() {
        if (contentComponent.isFocusOwner) {
            invisibleComponent.requestFocusTemporary()
            contentComponent.requestFocus()
        }
    }

    // region ContainerListener

    override fun componentAdded(e: ContainerEvent) {
        if (useInteropBlending) {
            return
        }
        val component = e.child
        val clipRectangle = ClipComponent(component)
        clipComponents[component] = clipRectangle
        clipRectangles.add(clipRectangle)
    }

    override fun componentRemoved(e: ContainerEvent) {
        val component = e.child
        clipComponents.remove(component)?.let {
            clipRectangles.remove(it)
        }
    }

    // endregion

    // region InputMethodListener

    override fun caretPositionChanged(event: InputMethodEvent?) {
        if (isDisposed) return
        // Which OSes and which input method could produce such events? We need to have some
        // specific cases in mind before implementing this
    }

    override fun inputMethodTextChanged(event: InputMethodEvent) {
        if (isDisposed) return
        catchExceptions {
            textInputService.inputMethodTextChanged(event)
        }
    }

    // endregion

    // region FocusListener

    override fun focusGained(e: FocusEvent) {
        // We don't reset focus for Compose when the component loses focus temporary.
        // Partially because we don't support restoring focus after clearing it.
        // Focus can be lost temporary when another window or popup takes focus.
        if (!e.isTemporary) {
            scene.focusManager.requestFocus()
        }
    }

    override fun focusLost(e: FocusEvent) {
        // We don't reset focus for Compose when the component loses focus temporary.
        // Partially because we don't support restoring focus after clearing it.
        // Focus can be lost temporary when another window or popup takes focus.
        if (!e.isTemporary) {
            scene.focusManager.releaseFocus()
        }
    }

    // endregion

    // region MouseListener

    override fun mouseClicked(event: MouseEvent) = Unit
    override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
    override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
    override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
    override fun mouseExited(event: MouseEvent) = onMouseEvent(event)

    // endregion

    // region MouseMotionListener

    override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
    override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)

    // endregion

    // region MouseWheelListener

    override fun mouseWheelMoved(event: MouseWheelEvent) {
        onMouseWheelEvent(event)
    }

    // endregion

    // region KeyListener

    override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
    override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
    override fun keyTyped(event: KeyEvent) = onKeyEvent(event)

    // endregion

    private inner class DesktopSkikoView : SkikoView {
        override val input: SkikoInput
            get() = SkikoInput.Empty

        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            catchExceptions {
                val composeCanvas = canvas.asComposeCanvas()
                val (dx, dy) = overrideOffset ?: scaledOffset
                composeCanvas.translate(-dx, -dy)
                scene.render(composeCanvas, nanoTime)
                composeCanvas.translate(dx, dy)
            }
        }
    }

    private inner class DesktopViewConfiguration : ViewConfiguration by EmptyViewConfiguration {
        override val touchSlop: Float get() = with(platformComponent.density) { 18.dp.toPx() }
    }

    private inner class DesktopFocusManager : FocusManager {
        override fun clearFocus(force: Boolean) {
            val root = contentComponent.rootPane
            root?.focusTraversalPolicy?.getDefaultComponent(root)?.requestFocusInWindow()
        }

        override fun moveFocus(focusDirection: FocusDirection): Boolean =
            when (focusDirection) {
                FocusDirection.Next -> {
                    val toFocus = contentComponent.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentAfter(root, contentComponent)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_FORWARD) == true
                }

                FocusDirection.Previous -> {
                    val toFocus = contentComponent.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentBefore(root, contentComponent)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_BACKWARD) == true
                }

                else -> false
            }
    }

    private inner class DesktopSemanticsOwnerListener : PlatformContext.SemanticsOwnerListener {
        /**
         * A new [SemanticsOwner] is always created above existing ones. So, usage of [LinkedHashMap]
         * is required here to keep insertion-order (that equal to [SemanticsOwner]s order).
         */
        private val _accessibilityControllers = linkedMapOf<SemanticsOwner, AccessibilityController>()
        val accessibilityControllers get() = _accessibilityControllers.values.reversed()

        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
            check(semanticsOwner !in _accessibilityControllers)
            _accessibilityControllers[semanticsOwner] = AccessibilityController(
                owner = semanticsOwner,
                desktopComponent = platformComponent,
                coroutineContext = coroutineContext,
                onFocusReceived = {
                    skiaLayerComponent.requestNativeFocusOnAccessible(it)
                }
            ).also {
                it.syncLoop()
            }
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers.remove(semanticsOwner)?.dispose()
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers[semanticsOwner]?.onSemanticsChange()
        }
    }

    private inner class DesktopPlatformContext : PlatformContext by PlatformContext.Empty {
        override val windowInfo by windowContext::windowInfo
        override var isWindowTransparent by windowContext::isWindowTransparent
        override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration()
        override val textInputService: PlatformTextInputService = this@ComposeSceneMediator.textInputService

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            contentComponent.cursor =
                (pointerIcon as? AwtCursor)?.cursor ?: Cursor(Cursor.DEFAULT_CURSOR)
        }
        override val parentFocusManager: FocusManager = DesktopFocusManager()
        override fun requestFocus(): Boolean {
            return contentComponent.hasFocus() || contentComponent.requestFocusInWindow()
        }

        override val rootForTestListener
            get() = this@ComposeSceneMediator.rootForTestListener
        override val semanticsOwnerListener
            get() = this@ComposeSceneMediator.semanticsOwnerListener
    }

    private inner class DesktopPlatformComponent : PlatformComponent {
        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            contentComponent.enableInputMethods(true)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            resetFocus()
        }

        override fun disableInput() {
            currentInputMethodRequests = null
            contentComponent.enableInputMethods(false)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            resetFocus()
        }

        override val locationOnScreen: Point
            get() = contentComponent.locationOnScreen

        override val density: Density
            get() = contentComponent.density
    }

    private class InvisibleComponent : Component() {
        fun requestFocusTemporary(): Boolean {
            return super.requestFocus(true)
        }
    }
}

private fun ComposeScene.onMouseEvent(
    offset: Offset,
    density: Density,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    val position = Offset(event.x.toFloat(), event.y.toFloat()) * density.density
    sendPointerEvent(
        eventType = eventType,
        position = position + offset,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event,
        button = event.getPointerButton()
    )
}

private fun MouseEvent.getPointerButton(): PointerButton? {
    if (button == MouseEvent.NOBUTTON) return null
    return when (button) {
        MouseEvent.BUTTON2 -> PointerButton.Tertiary
        MouseEvent.BUTTON3 -> PointerButton.Secondary
        else -> PointerButton(button - 1)
    }
}

private fun ComposeScene.onMouseWheelEvent(
    offset: Offset,
    density: Density,
    event: MouseWheelEvent
) {
    val position = Offset(event.x.toFloat(), event.y.toFloat()) * density.density
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = position + offset,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}


private val MouseEvent.buttons get() = PointerButtons(
    // We should check [event.button] because of case where [event.modifiersEx] does not provide
    // info about the pressed mouse button when using touchpad on MacOS 12 (AWT only).
    // When the [Tap to click] feature is activated on Mac OS 12, half of all clicks are not
    // handled because [event.modifiersEx] may not provide info about the pressed mouse button.
    isPrimaryPressed = ((modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON1))
        && !isMacOsCtrlClick,
    isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON3)
        || isMacOsCtrlClick,
    isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON2),
    isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 4),
    isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 5),
)

private val MouseEvent.keyboardModifiers get() = PointerKeyboardModifiers(
    isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
    isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
    isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
    isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
    isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
    isSymPressed = false,
    isFunctionPressed = false,
    isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
    isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
    isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
)

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
        hostOs.isMacOS &&
            ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
            ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
        )
