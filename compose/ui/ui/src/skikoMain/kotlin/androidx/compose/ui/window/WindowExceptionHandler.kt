package androidx.compose.ui.window

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Catch exceptions during rendering frames, handling events, or processing background Compose operations.
 */
@ExperimentalComposeUiApi
fun interface WindowExceptionHandler {
    /**
     * Called synchronously in UI thread when an exception occurred  during rendering frames,
     * handling events, or processing background Compose operations.
     */
    fun onException(throwable: Throwable)
}