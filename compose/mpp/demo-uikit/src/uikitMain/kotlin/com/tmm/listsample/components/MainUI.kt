package com.tmm.listsample.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.tmm.listsample.components.refresh.*
import com.tmm.listsample.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// just for demo
internal var models: MutableList<IBaseViewModel> = mutableStateListOf()

// must be internal otherwise iOS will fail to compile
@Composable
internal fun MainUiNoImageUseModel() {
    MaterialTheme {
        MainLazyColumnItemsList(noImage = true, useJson = false)
        DisposableEffect(Unit) {
            onDispose {
                models.clear()
            }
        }
    }
}

@Composable
internal fun MainLazyColumnItemsList(noImage: Boolean, useJson: Boolean) {
    val scope = rememberCoroutineScope()
    val state = rememberSwipeRefreshState(NORMAL)

    LaunchedEffect(scope) {
        scope.launch(Dispatchers.Default) {
            fetchCompositionModels(false) { list ->
                for (item in list)
                    models.add(item)
            }
        }
    }

    SwipeRefreshLayout(
        state = state,
        indicator = { modifier, s, indicatorHeight ->
            LoadingIndicatorDefault(modifier, s, indicatorHeight)
        },
        onRefresh = {
            scope.launch {
                state.loadState = REFRESHING
                //模拟网络请求
                delay(2000)
                fetchCompositionModels(useJson) {
                    models.clear()
                    for (item in it)
                        models.add(item)
                    state.loadState = NORMAL
                }
            }

        },
        onLoadMore = {
            scope.launch {
                state.loadState = LOADING_MORE
                delay(2000L)
                fetchCompositionModels(useJson) {
                    for (item in it)
                        models.add(item)
                    state.loadState = NORMAL
                }
            }
        }
    ) { modifier ->
        LazyColumn(modifier) {
            itemsIndexed(
                items = models,
                key = { index, _ ->
                    models[index]
                }
            ) { _, item ->
                when (item) {
                    is ICompositionModel -> MultiCellUI(item)
                    // .. todo need more types
                    else -> throw RuntimeException("Unexpected")
                }
            }
        }
    }
}

@Composable
internal fun MultiCellUI(item: IBaseViewModel) {
    DecoratedCell(item)
}