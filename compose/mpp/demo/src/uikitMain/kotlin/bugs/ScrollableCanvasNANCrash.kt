/*
 * Copyright 2023 The Android Open Source Project
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

package bugs

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.*
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
val ScrollableCanvasNANCrash = Screen.Example("NaN crash") {
    val pagerState = rememberPagerState(
        initialPage = 0, initialPageOffsetFraction = 0f
    ) {
        10
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(Modifier.height(20.dp))

        Button(onClick = {}) { Text(text = "Back") }

        Spacer(Modifier.height(10.dp))

        Text(text = "This is the second")

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(294.dp).background(Color.Blue),
            pageSpacing = 8.dp,
        ) { pageIndex ->
            Card(pageIndex)
        }
    }
}

@Composable
private fun Card(
    index: Int,
) {
    Column(
        modifier = Modifier.padding(start = 34.dp, end = 34.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize(1f).background(Color.Black)) {

            drawRect(
                topLeft = Offset(
                    x = 0f,
                    y = 10f,
                ),
                color = Color.DarkGray,
                size = Size(0.dp.toPx(), 0f),
            )
            drawRect(
                topLeft = Offset(
                    x = index * 100f,
                    y = 10f,
                ),
                color = Color.White,
                size = Size(1.dp.toPx(), index * 10f),
            )
        }
    }
}