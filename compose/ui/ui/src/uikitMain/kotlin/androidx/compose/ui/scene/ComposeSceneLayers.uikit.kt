/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.uikit.layoutConstraintsToMatch
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropMergedState
import androidx.compose.ui.viewinterop.UIKitInteropState
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.viewinterop.isEmpty
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import org.jetbrains.skia.Canvas
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UIWindow

internal class ComposeSceneLayers {
    val hasInvalidations: Boolean
        get() = layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()
    private var ongoingGesturesCount = 0

    /**
     * State of interop sessions managed by underlying layers.
     * Used for tracking the state of interop sessions and merging their transactions into one, so
     * that they can be consumed by the [MetalView].
     */
    private var interopState: InteropState = InteropState.Inactive(null)

    val view = ComposeSceneLayersView(
        ::onLayoutSubviews
    )

    val metalView: MetalView = MetalView(
        ::retrieveAndMergeInteropTransactions,
        ::render
    ).apply {
        canBeOpaque = false
    }

    init {
        view.translatesAutoresizingMaskIntoConstraints = false

        metalView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(metalView)

        NSLayoutConstraint.activateConstraints(
            metalView.layoutConstraintsToMatch(view)
        )
    }

    /**
     * When there is an ongoing gesture, we need notify redrawer about it. It should unconditionally
     * unpause CADisplayLink which affects frequency of polling UITouch events on high frequency
     * display and force it to match display refresh rate.
     *
     * Otherwise [UIEvent]s will be dispatched with the 60hz frequency.
     */
    fun onGestureEvent(event: GestureEvent) {
        val hadAnyOngoingGestures = ongoingGesturesCount > 0

        when (event) {
            GestureEvent.BEGAN -> {
                ongoingGesturesCount++

                if (!hadAnyOngoingGestures && ongoingGesturesCount > 0) {
                    metalView.needsProactiveDisplayLink = true
                }
            }
            GestureEvent.ENDED -> {
                ongoingGesturesCount--

                if (hadAnyOngoingGestures && ongoingGesturesCount == 0) {
                    metalView.needsProactiveDisplayLink = false
                }
            }
        }
    }

    fun dispose(hasViewAppeared: Boolean) {
        metalView.dispose()

        // `dispose` is called instead of `close`, because `close` is also used imperatively
        // to remove the layer from the array based on user interaction.
        while (layers.isNotEmpty()) {
            val layer = layers.removeLast()

            if (hasViewAppeared) {
                layer.sceneWillDisappear()
            }

            layer.dispose()
        }

        view.removeFromSuperview()
    }

    fun attach(window: UIWindow, layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        val isFirstLayer = layers.isEmpty()

        layers.add(layer)

        view.insertSubview(layer.view, belowSubview = metalView)
        NSLayoutConstraint.activateConstraints(
            layer.view.layoutConstraintsToMatch(view)
        )

        if (isFirstLayer) {
            // The content of previous layers drawn on the Metal view should be cleared and
            // redrawn synchronously after the new layer is attached to avoid flickering.

            metalView.setNeedsSynchronousDrawOnNextLayout()

            window.addSubview(view)
            NSLayoutConstraint.activateConstraints(
                view.layoutConstraintsToMatch(window)
            )
            window.layoutIfNeeded()
        }

        if (hasViewAppeared) {
            layer.sceneDidAppear()
        }

        layer.updateBasedOnView(view)
    }

    fun detach(layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        if (hasViewAppeared) {
            layer.sceneWillDisappear()
        }

        layers.remove(layer)

        // Intercept the actions UIKitInteropTransaction from the layer
        val transaction = layer.retrieveInteropTransaction()

        if (layers.isEmpty()) {
            view.removeFromSuperview()

            transaction.actions.fastForEach { it.invoke() }
            interopState = InteropState.Inactive(null)
        } else {
            // Redraw content with layer removed
            metalView.setNeedsRedraw()

            when (val state = interopState) {
                is InteropState.Inactive -> {
                    interopState = InteropState.Inactive(
                        uncommitedActions = transaction.actions
                    )
                }
                is InteropState.Active -> {
                    if (transaction.isEmpty()) {
                        interopState = InteropState.Active(
                            sessionsCount = state.sessionsCount,
                            uncommitedActions = transaction.actions
                        )
                    } else {
                        interopState = InteropState.Active(
                            sessionsCount = state.sessionsCount - 1,
                            uncommitedActions = transaction.actions
                        )
                    }
                }
            }
        }
    }

    private fun onLayoutSubviews() {
        layers.fastForEach {
            it.updateBasedOnView(view)
        }
    }

    fun viewDidAppear() {
        layers.fastForEach {
            it.sceneDidAppear()
        }
    }

    fun viewWillDisappear() {
        layers.fastForEach {
            it.sceneWillDisappear()
        }
    }

    /**
     * Iterate through existing layers and merge their interop transactions to be consumed by the
     * [MetalView]
     */
    private fun retrieveAndMergeInteropTransactions(): UIKitInteropTransaction =
        UIKitInteropTransaction.merge(
            transactions = layers.map { it.mediator.retrieveInteropTransaction() },
            mergedStateForActiveSessionsCountChange = ::mergedStateForActiveSessionsCountChange
        )

    /**
     * Update the tracker of internal state of interop sessions based on the [delta] of active
     * sessions count. Return the merged state of all sessions to be consumed by the [metalView].
     *
     * @param delta the change in active sessions count.
     */
    private fun mergedStateForActiveSessionsCountChange(delta: Int): UIKitInteropMergedState {
        if (delta == 0) {
            when (val state = interopState) {
                is InteropState.Inactive -> {
                    if (state.uncommitedActions == null) {
                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Unchanged,
                            additionalActions = emptyList()
                        )
                    } else {
                        // Interop session was ended by closing the last layer, but there are still
                        // uncommited actions, that need to be consumed, UIKitInteropState is still
                        // in the Began state so it needs to be Ended
                        interopState = InteropState.Inactive(uncommitedActions = null)
                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Ended,
                            additionalActions = state.uncommitedActions
                        )
                    }
                }
                is InteropState.Active -> {
                    if (state.uncommitedActions == null) {
                        // Total amount of interop sessions is unchanged, no additional actions
                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Unchanged,
                            additionalActions = emptyList()
                        )
                    } else {
                        // One of the layer was closed, but a new one was opened in the same frame
                        // The state is Unchanged and there are additional actions to be consumed
                        // and executed
                        interopState = InteropState.Active(
                            sessionsCount = state.sessionsCount,
                            uncommitedActions = null
                        )

                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Unchanged,
                            additionalActions = state.uncommitedActions
                        )
                    }
                }
            }
        } else {
            when (val state = interopState) {
                is InteropState.Active -> {
                    val newSessionsCount = state.sessionsCount + delta

                    if (newSessionsCount == 0) {
                        // All interop sessions ended
                        interopState = InteropState.Inactive(
                            uncommitedActions = null
                        )

                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Ended,
                            additionalActions = state.uncommitedActions ?: emptyList()
                        )
                    } else if (newSessionsCount > 0) {
                        // Some interop sessions are still active
                        interopState = InteropState.Active(
                            sessionsCount = newSessionsCount,
                            uncommitedActions = null
                        )

                        return UIKitInteropMergedState(
                            state = UIKitInteropState.Unchanged,
                            additionalActions = state.uncommitedActions ?: emptyList()
                        )
                    } else {
                        error("Invalid state: ComposeSceneLayers.InteropState.newSessionsCount is $newSessionsCount, must be non-negative")
                    }
                }

                is InteropState.Inactive -> {
                    if (delta > 0) {
                        interopState = InteropState.Active(
                            sessionsCount = delta,
                            uncommitedActions = null
                        )

                        return if (state.uncommitedActions == null) {
                            // New interop session started, no uncommited actions present,
                            // UIKitInteropState will be Began
                            UIKitInteropMergedState(
                                state = UIKitInteropState.Began,
                                additionalActions = emptyList()
                            )
                        } else {
                            // New interop session started but there are uncommitted actions from
                            // the last layer that was removed, UIKitInteropState is still Began
                            // so we can just pass Unchanged state with consumed actions

                            UIKitInteropMergedState(
                                state = UIKitInteropState.Began,
                                additionalActions = state.uncommitedActions
                            )
                        }
                    } else {
                        error("Invalid state: ComposeSceneLayers.InteropState.newSessionsCount is $delta, must be non-negative")
                    }
                }
            }
        }
    }

    private fun render(canvas: Canvas, nanoTime: Long) {
        val composeCanvas = canvas.asComposeCanvas()

        // Some layers may be removed during rendering, because recomposition will happen in the
        // process, so we need to make a copy of the list
        val layersCopy = layers.map { it }
        layersCopy.fastForEach {
            it.render(composeCanvas, nanoTime)
        }
    }

    /**
     * Sealed class aggregating the states of interop sessions managed by underlying layers
     */
    private sealed interface InteropState {
        /**
         * State when there are no active interop sessions
         *
         * @param uncommitedActions uncommited actions of the last layers that were removed
         * logically but were not yet executed. If null, current [UIKitInteropState] is
         * [UIKitInteropState.Ended], otherwise the strategy of [UIKitInteropState.Began] is still
         * used until all actions are executed.
         */
        class Inactive(
            val uncommitedActions: List<UIKitInteropAction>?
        ) : InteropState

        /**
         * State when there are active interop sessions.
         *
         * @param sessionsCount the amount of active interop sessions
         */
        class Active(
            val sessionsCount: Int,
            val uncommitedActions: List<UIKitInteropAction>?
        ) : InteropState
    }
}