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

<<<<<<<< HEAD:compose/runtime/runtime/src/nonJvmMain/kotlin/androidx/compose/runtime/SynchronizedObject.nonJvm.kt
package androidx.compose.runtime

internal actual class SynchronizedObject : kotlinx.atomicfu.locks.SynchronizedObject()

@PublishedApi
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R =
    kotlinx.atomicfu.locks.synchronized(lock, block)
========
@file:JvmName("ActualJvm_jvmKt")
@file:JvmMultifileClass

package androidx.compose.runtime

internal actual class SynchronizedObject

@PublishedApi
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R =
    kotlin.synchronized(lock, block)
>>>>>>>> origin/jb-main:compose/runtime/runtime/src/jvmMain/kotlin/androidx/compose/runtime/SynchronizedObject.jvm.kt
