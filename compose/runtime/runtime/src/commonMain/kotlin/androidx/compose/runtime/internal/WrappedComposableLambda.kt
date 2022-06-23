/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer

/**
 * Creates and remembers an instance of [ComposableLambda] and an instance of a wrapping lambda.
 * The wrapping lambda invokes the [ComposableLambda] with [invokeArgumentsCount].
 *
 * This function is used by compose compiler plugin (see WrapComposableLambdaLowering).
 * This function is a "twin" of [composableLambda].
 *
 * @return the wrapping lambda.
 */
@Suppress("unused")
@ComposeCompilerApi
internal fun wrappedComposableLambda(
    composer: Composer,
    key: Int,
    tracked: Boolean,
    invokeArgumentsCount: Int,
    block: Any
): Any {
    composer.startReplaceableGroup(key)

    // A small copy-paste from composableLambda:
    // It helps to reuse the same group to `remember` a ComposableLambda instance and its wrapper.
    // And this makes it explicit that ComposableLambda instance is the same within the group.
    val slotComposableLambda = composer.rememberedValue()
    val composableLambda = if (slotComposableLambda === Composer.Empty) {
        val value = ComposableLambdaImpl(key, tracked)
        composer.updateRememberedValue(value)
        value
    } else {
        slotComposableLambda as ComposableLambdaImpl
    }
    composableLambda.update(block)

    val slotWrapper = composer.rememberedValue()
    val wrapperInstance = if (slotWrapper === Composer.Empty) {
        // It's safe to create a lambda with composableLambda captured -
        // composableLambda instance never changes within the group.
        val value = wrapComposableLambda(composableLambda, invokeArgumentsCount)
        composer.updateRememberedValue(value)
        value
    } else {
        slotWrapper
    }

    composer.endReplaceableGroup()
    return wrapperInstance!!
}

/**
 * Wrap the [composableLambda] into a lambda with [invokeArgumentsCount] parameters.
 * The wrapping lambda invokes [composableLambda].
 *
 * This function is used by compose compiler plugin (see WrapComposableLambdaLowering).
 * It's expected that, [composableLambda] is created using [composableLambdaInstance].
 *
 * @return the wrapping lambda.
 */
@Suppress("unused")
@ComposeCompilerApi
internal fun wrappedComposableLambdaInstance(
    invokeArgumentsCount: Int,
    composableLambda: ComposableLambda
): Any {
    return wrapComposableLambda(composableLambda, invokeArgumentsCount)!!
}

private fun wrapComposableLambda(
    composableLambda: ComposableLambda,
    invokeArgumentsCount: Int
): Any? {
    return when (invokeArgumentsCount) {
        2 -> {
            val l: (Composer, Int) -> Any? = { composer, changed ->
                composableLambda.invoke(composer, changed)
            }
            l
        }
        3 -> {
            val l: (Any?, Composer, Int) -> Any? = { p1, composer, changed ->
                composableLambda.invoke(p1, composer, changed)
            }
            l
        }
        4 -> {
            val l: (Any?, Any?, Composer, Int) -> Any? = { p1, p2, composer, changed ->
                composableLambda.invoke(p1, p2, composer, changed)
            }
            l
        }
        5 -> {
            val l: (Any?, Any?, Any?, Composer, Int) -> Any? = { p1, p2, p3, composer, changed ->
                composableLambda.invoke(p1, p2, p3, composer, changed)
            }
            l
        }
        6 -> {
            val l: (Any?, Any?, Any?, Any?, Composer, Int) -> Any? = { p1, p2, p3, p4, composer, changed ->
                composableLambda.invoke(p1, p2, p3, p4, composer, changed)
            }
            l
        }
        7 -> {
            val l: (Any?, Any?, Any?, Any?, Any?, Composer, Int) -> Any? = { p1, p2, p3, p4, p5, composer, changed ->
                composableLambda.invoke(p1, p2, p3, p4, p5, composer, changed)
            }
            l
        }
        8 -> {
            val l: (Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int) -> Any? = { p1, p2, p3, p4, p5, p6, composer, changed ->
                composableLambda.invoke(p1, p2, p3, p4, p5, p6, composer, changed)
            }
            l
        }
        else -> null
    }
}
