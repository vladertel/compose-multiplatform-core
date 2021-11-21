package androidx.compose.ui.awt

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Default [WindowExceptionHandlerFactory], which will show standard error dialog, and close the window after that
 */
@ExperimentalComposeUiApi
object DefaultWindowExceptionHandleFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window) = Thread.UncaughtExceptionHandler { _, throwable ->
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, "An error has occurred", "Error", JOptionPane.ERROR_MESSAGE)
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        }
        throw throwable
    }
}
/**
 * Factory of window exception handlers.
 * These handlers catch exceptions during rendering frames, handling events, or processing background Compose operations.
 */
@ExperimentalComposeUiApi
interface WindowExceptionHandlerFactory {
    fun exceptionHandler(window: Window): Thread.UncaughtExceptionHandler
}

/**
 * The CompositionLocal that provides [WindowExceptionHandlerFactory].
 */
@ExperimentalComposeUiApi
val LocalWindowExceptionHandlerFactory = staticCompositionLocalOf { DefaultWindowExceptionHandleFactory }