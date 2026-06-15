package com.github.soundpod.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.innertube.Innertube
import com.github.innertube.utils.plus
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.ui.components.GridOverlay
import com.github.soundpod.ui.components.ShimmerHost
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.viewmodels.ItemsPageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun <T : Innertube.Item> ItemsPage(
    tag: String,
    header: (@Composable LazyGridItemScope.(allItems: List<T>) -> Unit)? = null,
    sortTransform: ((List<T>) -> List<T>)? = null,
    sortKeys: List<Any?> = emptyList(),
    enableScrollbar: Boolean = false,
    itemContent: @Composable LazyGridItemScope.(item: T, index: Int, allItems: List<T>) -> Unit,
    itemPlaceholderContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialPlaceholderCount: Int = 8,
    continuationPlaceholderCount: Int = 3,
    emptyItemsText: String = stringResource(id = R.string.no_items_found),
    itemsPageProvider: (suspend (String?) -> Result<Innertube.ItemsPage<T>?>?)? = null,
    enablePreCache: Boolean = false,
) {
    val playerPadding = LocalPlayerPadding.current
    val binder = LocalPlayerServiceBinder.current

    val updatedItemsPageProvider by rememberUpdatedState(itemsPageProvider)
    val lazyGridState = rememberLazyGridState()
    val viewModel: ItemsPageViewModel<T> = viewModel()
    val itemsPage: Innertube.ItemsPage<T>? =
        viewModel.itemsMap.getOrDefault(key = tag, defaultValue = null)
    
    val allItems by remember(itemsPage?.items, *sortKeys.toTypedArray()) {
        derivedStateOf {
            val items = itemsPage?.items ?: emptyList()
            sortTransform?.invoke(items) ?: items
        }
    }

    val filteredItems by remember(allItems) {
        derivedStateOf {
            allItems.filter { it.key.isNotEmpty() }.distinctBy { it.key }
        }
    }

    val listLayout = tag.contains("songs", ignoreCase = true) || 
                     tag.contains("videos", ignoreCase = true) || 
                     tag.contains("list", ignoreCase = true)
    val artistsLayout = tag.contains("artists")

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItemIndex = lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = lazyGridState.layoutInfo.totalItemsCount
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(filteredItems, enablePreCache) {
        if (enablePreCache && filteredItems.isNotEmpty()) {
            val videoIds = filteredItems.take(5).map { it.key }.filter { it.isNotEmpty() }
            if (videoIds.isNotEmpty()) {
                binder?.preCacheManager?.preCache(videoIds)
            }
        }
    }

    LaunchedEffect(shouldLoadMore, updatedItemsPageProvider, itemsPage?.continuation) {
        if (!shouldLoadMore) return@LaunchedEffect
        val currentItemsPageProvider = updatedItemsPageProvider ?: return@LaunchedEffect
        val continuation = itemsPage?.continuation ?: if (itemsPage != null) return@LaunchedEffect else null

        withContext(Dispatchers.IO) {
            currentItemsPageProvider(continuation)
        }?.onSuccess {
            if (it == null) {
                if (itemsPage == null) {
                    viewModel.setItems(
                        tag = tag,
                        items = Innertube.ItemsPage(items = null, continuation = null)
                    )
                }
            } else {
                viewModel.setItems(
                    tag = tag,
                    items = itemsPage + it
                )
            }
        }
    }

    GridOverlay(
        modifier = modifier,
        lazyGridState = lazyGridState,
        enableScrollbar = enableScrollbar,
        enableScrollToTop = false
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(
                minSize = if (listLayout) 400.dp else if (artistsLayout) 100.dp else 150.dp
            ),
            contentPadding = PaddingValues(
                start = if (listLayout) 0.dp else 8.dp,
                top = 8.dp,
                end = if (listLayout) 0.dp else 8.dp,
                bottom = 16.dp + playerPadding
            ),
            verticalArrangement = Arrangement.spacedBy(if (listLayout) 0.dp else 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (header != null) {
                item(
                    key = "header",
                    span = { GridItemSpan(maxCurrentLineSpan) }
                ) {
                    header(filteredItems)
                }
            }

            item(
                key = "anchor",
                span = { GridItemSpan(maxCurrentLineSpan) }
            ) {
                Spacer(modifier = Modifier.height(Dp.Hairline))
            }

            itemsIndexed(
                items = filteredItems,
                key = { _, item -> item.key },
                itemContent = { index, item ->
                    this.itemContent(item, index, filteredItems)
                }
            )

            if (itemsPage != null && itemsPage.items.isNullOrEmpty()) {
                item(
                    key = "empty",
                    span = { GridItemSpan(maxCurrentLineSpan) }
                ) {
                    Text(
                        text = emptyItemsText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                            .fillMaxWidth()
                            .alpha(Dimensions.MEDIUMOPACITY)
                    )
                }
            }

            if (itemsPage == null || itemsPage.continuation != null) {
                val isFirstLoad = itemsPage?.items.isNullOrEmpty()

                items(
                    count = if (isFirstLoad) initialPlaceholderCount else continuationPlaceholderCount,
                    key = { "loading$it" }
                ) {
                    ShimmerHost {
                        itemPlaceholderContent()
                    }
                }
            }
        }
    }
}
