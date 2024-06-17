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

@file:JvmName("ComposableLambdaKt")
@file:JvmMultifileClass
package androidx.compose.runtime.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.remember
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// TODO (o.karpovich): move this function back to common ComposableLambda.kt when compose compiler is fixed for web
// see https://youtrack.jetbrains.com/issue/CMP-1547
@Suppress("unused")
@Composable
@ComposeCompilerApi
fun rememberComposableLambda(
    key: Int,
    tracked: Boolean,
    block: Any
): ComposableLambda = remember { ComposableLambdaImpl(key, tracked, block) }.also {
    it.update(block)
}