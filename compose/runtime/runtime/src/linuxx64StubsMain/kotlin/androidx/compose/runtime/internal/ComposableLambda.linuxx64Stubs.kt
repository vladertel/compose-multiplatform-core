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

package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.implementedInJetBrainsFork

@Suppress("NAME_SHADOWING")
@Stable
/* ktlint-disable parameter-list-wrapping */ // TODO(https://github.com/pinterest/ktlint/issues/921): reenable
internal actual class ComposableLambdaImpl actual constructor(
    key: Int,
    tracked: Boolean,
    block: Any?,
) : ComposableLambda {
    actual fun update(block: Any): Unit = implementedInJetBrainsFork()

    actual override operator fun invoke(c: Composer, changed: Int): Any? =
            implementedInJetBrainsFork()

    actual override operator fun invoke(p1: Any?, c: Composer, changed: Int): Any? =
            implementedInJetBrainsFork()

    actual override operator fun invoke(p1: Any?, p2: Any?, c: Composer, changed: Int): Any? =
            implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        c: Composer,
        changed: Int
    ): Any? =
            implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        c: Composer,
        changed: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()

    actual override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        p18: Any?,
        c: Composer,
        changed: Int,
        changed1: Int
    ): Any? = implementedInJetBrainsFork()
}

@ComposeCompilerApi
@Stable
actual interface ComposableLambda :
    Function2<Composer, Int, Any?>,
    Function3<Any?, Composer, Int, Any?>,
    Function4<Any?, Any?, Composer, Int, Any?>,
    Function5<Any?, Any?, Any?, Composer, Int, Any?>,
    Function6<Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function7<Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function8<Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function11<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function13<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function14<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function15<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function16<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function17<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function18<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function19<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function20<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function21<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >
