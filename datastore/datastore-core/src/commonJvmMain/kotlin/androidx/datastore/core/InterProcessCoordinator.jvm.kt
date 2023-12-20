/*
 * Copyright 2021 The Android Open Source Project
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

<<<<<<<< HEAD:compose/foundation/foundation/src/jsWasmMain/kotlin/androidx/compose/foundation/text/selection/SelectionHandles.js.kt
package androidx.compose.foundation.text.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.ResolvedTextDirection

@Composable
internal actual fun SelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
    content: (@Composable () -> Unit)?
) {
    // TODO
}
========
package androidx.datastore.core

import java.io.File

/**
 * Create a coordinator for single process use cases.
 *
 * @param file The canonical file managed by [SingleProcessCoordinator]
 */
@Suppress("StreamFiles")
fun createSingleProcessCoordinator(file: File): InterProcessCoordinator =
    createSingleProcessCoordinator(file.canonicalFile.absolutePath)
>>>>>>>> jetpack-compose/1.6.0-beta03:datastore/datastore-core/src/commonJvmMain/kotlin/androidx/datastore/core/InterProcessCoordinator.jvm.kt
