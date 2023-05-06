@file:Suppress("LongMethod", "MagicNumber")

package com.fibelatti.pinboard.features.tags.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.core.android.composable.EmptyListContent
import com.fibelatti.pinboard.core.android.composable.rememberAutoDismissPullRefreshState
import com.fibelatti.pinboard.features.tags.domain.model.Tag
import com.fibelatti.pinboard.features.tags.domain.model.TagSorting
import com.fibelatti.ui.components.RowToggleButtonGroup
import com.fibelatti.ui.components.ToggleButtonGroup
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.launch

@Composable
fun TagList(
    modifier: Modifier = Modifier,
    tagsViewModel: TagsViewModel = hiltViewModel(),
    onTagClicked: (Tag) -> Unit = {},
    onTagLongClicked: (Tag) -> Unit = {},
    onPullToRefresh: () -> Unit = {},
) {
    val state by tagsViewModel.state.collectAsStateWithLifecycle()

    TagList(
        items = state.filteredTags,
        isLoading = state.isLoading,
        modifier = modifier,
        onSortOptionClicked = { sorting ->
            tagsViewModel.sortTags(
                sorting = when (sorting) {
                    TagList.Sorting.Alphabetically -> TagSorting.AtoZ
                    TagList.Sorting.MoreFirst -> TagSorting.MoreFirst
                    TagList.Sorting.LessFirst -> TagSorting.LessFirst
                    TagList.Sorting.Search -> TagSorting.AtoZ
                },
                searchQuery = "",
            )
        },
        searchInput = state.currentQuery,
        onSearchInputChanged = tagsViewModel::searchTags,
        onTagClicked = onTagClicked,
        onTagLongClicked = onTagLongClicked,
        onPullToRefresh = onPullToRefresh,
    )
}

@Composable
fun TagList(
    items: List<Tag>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSortOptionClicked: (TagList.Sorting) -> Unit = {},
    searchInput: String = "",
    onSearchInputChanged: (newValue: String) -> Unit = {},
    onSearchInputFocusChanged: (hasFocus: Boolean) -> Unit = {},
    onTagClicked: (Tag) -> Unit = {},
    onTagLongClicked: (Tag) -> Unit = {},
    onPullToRefresh: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (items.isEmpty() && searchInput.isBlank()) {
            EmptyListContent(
                icon = painterResource(id = R.drawable.ic_tag),
                title = stringResource(id = R.string.tags_empty_title),
                description = stringResource(id = R.string.tags_empty_description),
            )
        } else {
            TagList(
                items = items,
                onSortOptionClicked = onSortOptionClicked,
                searchInput = searchInput,
                onSearchInputChanged = onSearchInputChanged,
                onSearchInputFocusChanged = onSearchInputFocusChanged,
                onTagClicked = onTagClicked,
                onTagLongClicked = onTagLongClicked,
                onPullToRefresh = onPullToRefresh,
            )
        }
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
)
private fun TagList(
    items: List<Tag>,
    onSortOptionClicked: (TagList.Sorting) -> Unit,
    searchInput: String,
    onSearchInputChanged: (newValue: String) -> Unit,
    onSearchInputFocusChanged: (hasFocus: Boolean) -> Unit,
    onTagClicked: (Tag) -> Unit,
    onTagLongClicked: (Tag) -> Unit,
    onPullToRefresh: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showFilter by rememberSaveable { mutableStateOf(false) }

    RowToggleButtonGroup(
        items = TagList.Sorting.values().map { sorting ->
            ToggleButtonGroup.Item(
                id = sorting.id,
                text = stringResource(id = sorting.label),
            )
        },
        onButtonClick = {
            val sorting = requireNotNull(TagList.Sorting.findById(it.id))
            showFilter = sorting == TagList.Sorting.Search

            onSortOptionClicked(sorting)

            if (!showFilter) {
                onSearchInputChanged("")
                onSearchInputFocusChanged(false)
            }

            scope.launch {
                listState.scrollToItem(index = 0)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        selectedIndex = 0,
        buttonHeight = 40.dp,
        textStyle = MaterialTheme.typography.bodySmall,
    )

    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = showFilter,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        OutlinedTextField(
            value = searchInput,
            onValueChange = onSearchInputChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .onFocusChanged { onSearchInputFocusChanged(it.hasFocus) },
            label = { Text(text = stringResource(id = R.string.tag_filter_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions { focusManager.clearFocus() },
            singleLine = true,
            maxLines = 1,
        )
    }

    val (pullRefreshState, refreshing) = rememberAutoDismissPullRefreshState(onPullToRefresh)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets(bottom = 100.dp)
                .add(WindowInsets.navigationBars)
                .asPaddingValues(),
            state = listState,
        ) {
            items(items) { item ->
                TagListItem(
                    item = item,
                    onTagClicked = onTagClicked,
                    onTagLongClicked = onTagLongClicked,
                )
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            scale = true,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TagListItem(
    item: Tag,
    onTagClicked: (Tag) -> Unit,
    onTagLongClicked: (Tag) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onTagClicked(item) },
                onLongClick = { onTagLongClicked(item) },
            )
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = pluralStringResource(R.plurals.posts_quantity, item.posts, item.posts),
            color = MaterialTheme.colorScheme.onBackground,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

object TagList {

    enum class Sorting(val id: String, val label: Int) {

        Alphabetically(id = "alphabetically", label = R.string.tags_sorting_a_to_z),
        MoreFirst(id = "more-first", label = R.string.tags_sorting_more_first),
        LessFirst(id = "less-first", label = R.string.tags_sorting_less_first),
        Search(id = "search", label = R.string.tags_sorting_filter),
        ;

        companion object {

            fun findById(id: String): Sorting? = values().find { it.id == id }
        }
    }
}

@Composable
@ThemePreviews
private fun EmptyTagListPreview() {
    ExtendedTheme {
        TagList(
            items = emptyList(),
            isLoading = false,
        )
    }
}

@Composable
@ThemePreviews
private fun TagListPreview() {
    ExtendedTheme {
        TagList(
            items = List(size = 5) { Tag(name = "Tag $it", posts = it * it) },
            isLoading = false,
        )
    }
}
