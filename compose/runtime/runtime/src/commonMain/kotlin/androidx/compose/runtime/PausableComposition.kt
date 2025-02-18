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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.collection.emptyScatterSet
import androidx.collection.mutableIntListOf
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.internal.RememberEventDispatcher
import androidx.compose.runtime.platform.SynchronizedObject
import androidx.compose.runtime.platform.synchronized

/**
 * A [PausableComposition] is a sub-composition that can be composed incrementally as it supports
 * being paused and resumed.
 *
 * Pausable sub-composition can be used between frames to prepare a sub-composition before it is
 * required by the main composition. For example, this is used in lazy lists to prepare list items
 * in between frames to that are likely to be scrolled in. The composition is paused when the start
 * of the next frame is near allowing composition to be spread across multiple frames without
 * delaying the production of the next frame.
 *
 * The result of the composition should not be used (e.g. the nodes should not added to a layout
 * tree or placed in layout) until [PausedComposition.isComplete] is `true` and
 * [PausedComposition.apply] has been called. The composition is incomplete and will not
 * automatically recompose until after [PausedComposition.apply] is called.
 *
 * A [PausableComposition] is a [ReusableComposition] but [setPausableContent] should be used
 * instead of [ReusableComposition.setContentWithReuse] to create a paused composition.
 *
 * If [Composition.setContent] or [ReusableComposition.setContentWithReuse] are used then the
 * composition behaves as if it wasn't pausable. If there is a [PausedComposition] that has not yet
 * been applied, an exception is thrown.
 *
 * @see Composition
 * @see ReusableComposition
 */
sealed interface PausableComposition : ReusableComposition {
    /**
     * Set the content of the composition. A [PausedComposition] that is currently paused. No
     * composition is performed until [PausedComposition.resume] is called.
     * [PausedComposition.resume] should be called until [PausedComposition.isComplete] is `true`.
     * The composition should not be used until [PausedComposition.isComplete] is `true` and
     * [PausedComposition.apply] has been called.
     *
     * @see Composition.setContent
     * @see ReusableComposition.setContentWithReuse
     */
    fun setPausableContent(content: @Composable () -> Unit): PausedComposition

    /**
     * Set the content of a resuable composition. A [PausedComposition] that is currently paused. No
     * composition is performed until [PausedComposition.resume] is called.
     * [PausedComposition.resume] should be called until [PausedComposition.isComplete] is `true`.
     * The composition should not be used until [PausedComposition.isComplete] is `true` and
     * [PausedComposition.apply] has been called.
     *
     * @see Composition.setContent
     * @see ReusableComposition.setContentWithReuse
     */
    fun setPausableContentWithReuse(content: @Composable () -> Unit): PausedComposition
}

/** The callback type used in [PausedComposition.resume]. */
fun interface ShouldPauseCallback {
    /**
     * Called to determine if a resumed [PausedComposition] should pause.
     *
     * @return Return `true` to indicate that the composition should pause. Otherwise the
     *   composition will continue normally.
     */
    @Suppress("CallbackMethodName") fun shouldPause(): Boolean
}

/**
 * [PausedComposition] is the result of calling [PausableComposition.setContent] or
 * [PausableComposition.setContentWithReuse]. It is used to drive the paused composition to
 * completion. A [PausedComposition] should not be used until [isComplete] is `true` and [apply] has
 * been called.
 *
 * A [PausedComposition] is created paused and will only compose the `content` parameter when
 * [resume] is called the first time.
 */
sealed interface PausedComposition {
    /**
     * Returns `true` when the [PausedComposition] is complete. [isComplete] matches the last value
     * returned from [resume]. Once a [PausedComposition] is [isComplete] the [apply] method should
     * be called.
     */
    val isComplete: Boolean

    /**
     * Resume the composition that has been paused. This method should be called until [resume]
     * returns `true` or [isComplete] is `true` which has the same result as the last result of
     * calling [resume]. The [shouldPause] parameter is a lambda that returns whether the
     * composition should be paused. For example, in lazy lists this returns `false` until just
     * prior to the next frame starting in which it returns `true`
     *
     * Calling [resume] after it returns `true` or when `isComplete` is true will throw an
     * exception.
     *
     * @param shouldPause A lambda that is used to determine if the composition should be paused.
     *   This lambda is called often so should be a very simple calculation. Returning `true` does
     *   not guarantee the composition will pause, it should only be considered a request to pause
     *   the composition. Not all composable functions are pausable and only pausable composition
     *   functions will pause.
     * @return `true` if the composition is complete and `false` if one or more calls to `resume`
     *   are required to complete composition.
     */
    @Suppress("ExecutorRegistration") fun resume(shouldPause: ShouldPauseCallback): Boolean

    /**
     * Apply the composition. This is the last step of a paused composition and is required to be
     * called prior to the composition is usable.
     */
    fun apply()

    /**
     * Cancels the paused composition. This should only be used if the composition is going to be
     * disposed and the entire composition is not going to be used.
     */
    fun cancel()
}

/**
 * Create a [PausableComposition]. A [PausableComposition] can create a [PausedComposition] which
 * allows pausing and resuming the composition.
 *
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see CompositionContext
 * @see PausableComposition
 */
fun PausableComposition(applier: Applier<*>, parent: CompositionContext): PausableComposition =
    CompositionImpl(parent, applier)

internal enum class PausedCompositionState {
    Invalid,
    Cancelled,
    InitialPending,
    RecomposePending,
    ApplyPending,
    Applied,
}

internal class PausedCompositionImpl(
    val composition: CompositionImpl,
    val context: CompositionContext,
    val composer: ComposerImpl,
    abandonSet: MutableSet<RememberObserver>,
    val content: @Composable () -> Unit,
    val reusable: Boolean,
    val applier: Applier<*>,
    val lock: SynchronizedObject,
) : PausedComposition {
    private var state = PausedCompositionState.InitialPending
    private var invalidScopes = emptyScatterSet<RecomposeScopeImpl>()
    internal val rememberManager = RememberEventDispatcher(abandonSet)
    internal val pausableApplier = RecordingApplier(applier.current)

    override val isComplete: Boolean
        get() = state >= PausedCompositionState.ApplyPending

    override fun resume(shouldPause: ShouldPauseCallback): Boolean {
        try {
            when (state) {
                PausedCompositionState.InitialPending -> {
                    if (reusable) composer.startReuseFromRoot()
                    try {
                        invalidScopes =
                            context.composeInitialPaused(composition, shouldPause, content)
                    } finally {
                        if (reusable) composer.endReuseFromRoot()
                    }
                    state = PausedCompositionState.RecomposePending
                    if (invalidScopes.isEmpty()) markComplete()
                }
                PausedCompositionState.RecomposePending -> {
                    invalidScopes = context.recomposePaused(composition, shouldPause, invalidScopes)
                    if (invalidScopes.isEmpty()) markComplete()
                }
                PausedCompositionState.ApplyPending ->
                    error("Pausable composition is complete and apply() should be applied")
                PausedCompositionState.Applied -> error("The paused composition has been applied")
                PausedCompositionState.Cancelled ->
                    error("The paused composition has been cancelled")
                PausedCompositionState.Invalid ->
                    error("The paused composition is invalid because of a previous exception")
            }
        } catch (e: Exception) {
            state = PausedCompositionState.Invalid
        }
        return isComplete
    }

    override fun apply() {
        try {
            when (state) {
                PausedCompositionState.InitialPending,
                PausedCompositionState.RecomposePending ->
                    error("The paused composition has not completed yet")
                PausedCompositionState.ApplyPending -> {
                    applyChanges()
                    state = PausedCompositionState.Applied
                }
                PausedCompositionState.Applied ->
                    error("The paused composition has already been applied")
                PausedCompositionState.Cancelled ->
                    error("The paused composition has been cancelled")
                PausedCompositionState.Invalid ->
                    error("The paused composition is invalid because of a previous exception")
            }
        } catch (e: Exception) {
            state = PausedCompositionState.Invalid
            throw e
        }
    }

    override fun cancel() {
        state = PausedCompositionState.Cancelled
        rememberManager.dispatchAbandons()
        composition.pausedCompositionFinished()
    }

    private fun markComplete() {
        state = PausedCompositionState.ApplyPending
    }

    private fun applyChanges() {
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            try {
                pausableApplier.playTo(applier as Applier<Any?>)
                rememberManager.dispatchRememberObservers()
                rememberManager.dispatchSideEffects()
            } finally {
                rememberManager.dispatchAbandons()
                composition.pausedCompositionFinished()
            }
        }
    }
}

internal class RecordingApplier<N>(root: N) : Applier<N> {
    private val stack = mutableObjectListOf<N>()
    private val operations = mutableIntListOf()
    private val instances = mutableObjectListOf<Any?>()

    override var current: N = root

    override fun down(node: N) {
        operations.add(DOWN)
        instances.add(node)
        stack.add(current)
        current = node
    }

    override fun up() {
        operations.add(UP)
        current = stack.removeAt(stack.size - 1)
    }

    override fun remove(index: Int, count: Int) {
        operations.add(REMOVE)
        operations.add(index)
        operations.add(count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        operations.add(MOVE)
        operations.add(from)
        operations.add(to)
        operations.add(count)
    }

    override fun clear() {
        operations.add(CLEAR)
    }

    override fun insertBottomUp(index: Int, instance: N) {
        operations.add(INSERT_BOTTOM_UP)
        operations.add(index)
        instances.add(instance)
    }

    override fun insertTopDown(index: Int, instance: N) {
        operations.add(INSERT_TOP_DOWN)
        operations.add(index)
        instances.add(instance)
    }

    override fun apply(block: N.(Any?) -> Unit, value: Any?) {
        operations.add(APPLY)
        instances.add(block)
        instances.add(value)
    }

    override fun reuse() {
        operations.add(REUSE)
    }

    fun playTo(applier: Applier<N>) {
        var currentOperation = 0
        var currentInstance = 0
        val operations = operations
        val size = operations.size
        val instances = instances
        applier.onBeginChanges()
        try {
            while (currentOperation < size) {
                val operation = operations[currentOperation++]
                when (operation) {
                    UP -> {
                        applier.up()
                    }
                    DOWN -> {
                        @Suppress("UNCHECKED_CAST") val node = instances[currentInstance++] as N
                        applier.down(node)
                    }
                    REMOVE -> {
                        val index = operations[currentOperation++]
                        val count = operations[currentOperation++]
                        applier.remove(index, count)
                    }
                    MOVE -> {
                        val from = operations[currentOperation++]
                        val to = operations[currentOperation++]
                        val count = operations[currentOperation++]
                        applier.move(from, to, count)
                    }
                    CLEAR -> {
                        applier.clear()
                    }
                    INSERT_TOP_DOWN -> {
                        val index = operations[currentOperation++]

                        @Suppress("UNCHECKED_CAST") val instance = instances[currentInstance++] as N
                        applier.insertTopDown(index, instance)
                    }
                    INSERT_BOTTOM_UP -> {
                        val index = operations[currentOperation++]

                        @Suppress("UNCHECKED_CAST") val instance = instances[currentInstance++] as N
                        applier.insertBottomUp(index, instance)
                    }
                    APPLY -> {
                        @Suppress("UNCHECKED_CAST")
                        val block = instances[currentInstance++] as Any?.(Any?) -> Unit
                        val value = instances[currentInstance++]
                        applier.apply(block, value)
                    }
                    REUSE -> {
                        applier.reuse()
                    }
                }
            }
            runtimeCheck(currentInstance == instances.size) { "Applier operation size mismatch" }
            instances.clear()
            operations.clear()
        } finally {
            applier.onEndChanges()
        }
    }

    // These commands need to be an integer, not just a enum value, as they are stored along side
    // the commands integer parameters, so the values are explicitly set.
    companion object {
        const val UP = 0
        const val DOWN = UP + 1
        const val REMOVE = DOWN + 1
        const val MOVE = REMOVE + 1
        const val CLEAR = MOVE + 1
        const val INSERT_BOTTOM_UP = CLEAR + 1
        const val INSERT_TOP_DOWN = INSERT_BOTTOM_UP + 1
        const val APPLY = INSERT_TOP_DOWN + 1
        const val REUSE = APPLY + 1
    }
}
