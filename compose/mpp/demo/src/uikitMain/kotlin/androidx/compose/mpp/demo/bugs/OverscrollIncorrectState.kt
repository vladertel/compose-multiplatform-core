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

package androidx.compose.mpp.demo.bugs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.seconds

val OverscrollIncorrectState = Screen.Example("OverscrollIncorrectState #4935") {
    ScrollPage()
}

data class Data(val title: String)

@Immutable
data class DataList(val list: List<Data>)

var currentIndex by mutableStateOf(10)

var data by mutableStateOf(makeData(currentIndex))


private suspend fun loadMore() {
    delay(1.seconds)
    currentIndex += 10
    data = makeData(currentIndex)
}

private fun makeData(count: Int): DataList {
    return DataList((0..<count).map { Data("Item: $it") })
}

@Composable
private fun ScrollPage() {
    val state = rememberLazyListState()
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            layoutInfo.visibleItemsInfo.lastOrNull()?.let {
                it.index > layoutInfo.totalItemsCount - 3
            } ?: false
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { loadMore.value }.distinctUntilChanged().collect {
            loadMore()
        }
    }
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(data.list) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp)
                    .border(width = 1.dp, color = Color.Blue, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    it.title
                )
            }
        }
    }
}