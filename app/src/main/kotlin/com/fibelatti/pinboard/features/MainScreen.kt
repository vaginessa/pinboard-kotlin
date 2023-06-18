package com.fibelatti.pinboard.features

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.core.extension.createFragment
import com.fibelatti.core.extension.findActivity
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.core.android.composable.MainTitle
import com.fibelatti.pinboard.core.extension.ScrollDirection
import com.fibelatti.pinboard.features.appstate.AppStateViewModel
import com.fibelatti.pinboard.features.appstate.ConnectionAwareContent
import com.fibelatti.pinboard.features.appstate.LoginContent
import com.fibelatti.pinboard.features.appstate.PopularPostsContent
import com.fibelatti.pinboard.features.appstate.PostListContent
import com.fibelatti.pinboard.features.appstate.Refresh
import com.fibelatti.pinboard.features.appstate.RefreshPopular
import com.fibelatti.pinboard.features.navigation.NavigationMenuFragment
import com.fibelatti.ui.foundation.StableList
import com.fibelatti.ui.foundation.stableListOf
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun MainTopAppBar(
    mainViewModel: MainViewModel = hiltViewModel(),
    appStateViewModel: AppStateViewModel = hiltViewModel(),
) {
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    val content by appStateViewModel.content.collectAsStateWithLifecycle()

    MainTopAppBar(
        state = state,
        onNavigationClick = { mainViewModel.navigationClicked(state.navigation.id) },
        onActionButtonClick = { data -> mainViewModel.actionButtonClicked(state.actionButton.id, data) },
        isOffline = content.let { it is ConnectionAwareContent && !it.isConnected },
        showRetryButton = content is PostListContent || content is PopularPostsContent,
        onOfflineRetryClick = retryClick@{
            val action = when (content) {
                is PostListContent -> Refresh()
                is PopularPostsContent -> RefreshPopular
                else -> return@retryClick
            }

            appStateViewModel.runAction(action)
        },
        hideAllControls = content is LoginContent,
    )
}

@Composable
fun MainBottomAppBar(
    mainViewModel: MainViewModel = hiltViewModel(),
    appStateViewModel: AppStateViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val state by mainViewModel.state.collectAsStateWithLifecycle()
        val currentScrollDirection by mainViewModel.currentScrollDirection.collectAsStateWithLifecycle()

        val content by appStateViewModel.content.collectAsStateWithLifecycle()
        val hideAllControls by remember { derivedStateOf { content is LoginContent } }

        var bottomAppBarHeight by remember { mutableStateOf(0) }

        AnimatedVisibility(
            visible = !hideAllControls &&
                state.multiPanelEnabled &&
                state.multiPanelContent &&
                state.sidePanelAppBar is MainState.SidePanelAppBarComponent.Visible,
            modifier = if (state.bottomAppBar is MainState.BottomAppBarComponent.Visible) {
                Modifier.padding(bottom = with(LocalDensity.current) { bottomAppBarHeight.toDp() })
            } else {
                Modifier.systemBarsPadding()
            },
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            SidePanelBottomAppBar(
                state = state,
                onMenuItemClick = { menuItem, data ->
                    mainViewModel.menuItemClicked(id = state.sidePanelAppBar.id, menuItem = menuItem, data = data)
                },
            )
        }

        AnimatedVisibility(
            visible = !hideAllControls && currentScrollDirection != ScrollDirection.DOWN,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { bottomAppBarHeight = it.size.height },
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            val localContext = LocalContext.current

            MainBottomAppBar(
                state = state,
                onBottomNavClick = {
                    localContext.findActivity()?.run {
                        createFragment<NavigationMenuFragment>().show(
                            supportFragmentManager,
                            NavigationMenuFragment.TAG,
                        )
                    }
                },
                onMenuItemClick = { menuItem, data ->
                    mainViewModel.menuItemClicked(id = state.bottomAppBar.id, menuItem = menuItem, data = data)
                },
                onFabClick = { data ->
                    mainViewModel.fabClicked(id = state.floatingActionButton.id, data = data)
                },
            )
        }
    }
}

@Composable
private fun MainTopAppBar(
    state: MainState,
    onNavigationClick: () -> Unit,
    onActionButtonClick: (data: Any?) -> Unit,
    isOffline: Boolean,
    showRetryButton: Boolean,
    onOfflineRetryClick: () -> Unit,
    hideAllControls: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!hideAllControls) {
            MainTitle(
                title = state.title,
                subtitle = state.subtitle,
                navigation = state.navigation,
                onNavigationClicked = onNavigationClick,
                actionButton = state.actionButton,
                onActionButtonClicked = onActionButtonClick,
            )

            AnimatedVisibility(visible = isOffline) {
                OfflineAlert(
                    showRetryButton = showRetryButton,
                    onOfflineRetryClick = onOfflineRetryClick,
                )
            }
        }
    }
}

@Composable
private fun OfflineAlert(
    showRetryButton: Boolean,
    onOfflineRetryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.offline_alert),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (showRetryButton) {
            TextButton(onClick = onOfflineRetryClick) {
                Text(text = stringResource(id = R.string.offline_retry))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun MainBottomAppBar(
    state: MainState,
    onBottomNavClick: () -> Unit,
    onMenuItemClick: (MainState.MenuItemComponent, data: Any?) -> Unit,
    onFabClick: (data: Any?) -> Unit,
) {
    if (state.bottomAppBar is MainState.BottomAppBarComponent.Visible) {
        BottomAppBar(
            actions = {
                MainBottomAppBarMenu(
                    bottomAppBar = state.bottomAppBar,
                    onBottomNavClick = onBottomNavClick,
                    onMenuItemClick = onMenuItemClick,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            floatingActionButton = {
                if (state.floatingActionButton is MainState.FabComponent.Visible) {
                    FloatingActionButton(onClick = { onFabClick(state.floatingActionButton.data) }) {
                        AnimatedContent(
                            targetState = state.floatingActionButton.icon,
                            transitionSpec = { fadeIn() + scaleIn() with fadeOut() + scaleOut() },
                            label = "Fab_Icon",
                        ) { icon ->
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun MainBottomAppBarMenu(
    bottomAppBar: MainState.BottomAppBarComponent.Visible,
    onBottomNavClick: () -> Unit,
    onMenuItemClick: (MainState.MenuItemComponent, data: Any?) -> Unit,
) {
    if (bottomAppBar.navigationIcon != null) {
        IconButton(onClick = onBottomNavClick) {
            Icon(painter = painterResource(id = bottomAppBar.navigationIcon), contentDescription = null)
        }
    }

    MenuItemsContent(
        menuItems = bottomAppBar.menuItems,
        data = bottomAppBar.data,
        onMenuItemClick = onMenuItemClick,
    )
}

@Composable
private fun MenuItemsContent(
    menuItems: StableList<MainState.MenuItemComponent>,
    data: Any?,
    onMenuItemClick: (MainState.MenuItemComponent, data: Any?) -> Unit,
) {
    for (menuItem in menuItems.value) {
        if (menuItem.icon == null) {
            TextButton(
                onClick = { onMenuItemClick(menuItem, data) },
            ) {
                Text(
                    text = stringResource(id = menuItem.name),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        } else {
            IconButton(
                onClick = { onMenuItemClick(menuItem, data) },
            ) {
                Icon(
                    painter = painterResource(id = menuItem.icon),
                    contentDescription = stringResource(id = menuItem.name),
                )
            }
        }
    }
}

@Composable
private fun SidePanelBottomAppBar(
    state: MainState,
    onMenuItemClick: (MainState.MenuItemComponent, data: Any?) -> Unit,
) {
    if (state.sidePanelAppBar is MainState.SidePanelAppBarComponent.Visible) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(all = 8.dp),
        ) {
            MenuItemsContent(
                menuItems = state.sidePanelAppBar.menuItems,
                data = state.sidePanelAppBar.data,
                onMenuItemClick = onMenuItemClick,
            )
        }
    }
}

// region Previews
@Composable
@ThemePreviews
private fun MainTopAppBarPreview() {
    ExtendedTheme {
        MainTopAppBar(
            state = remember {
                MainState(
                    title = MainState.TitleComponent.Visible(label = "Sample title"),
                    subtitle = MainState.TitleComponent.Visible(label = "Sample subtitle"),
                    navigation = MainState.NavigationComponent.Visible(id = ""),
                    actionButton = MainState.ActionButtonComponent.Visible(id = "", label = "Action"),
                    bottomAppBar = MainState.BottomAppBarComponent.Visible(
                        id = "",
                        menuItems = stableListOf(MainState.MenuItemComponent.SearchBookmarks),
                        navigationIcon = R.drawable.ic_menu,
                    ),
                    floatingActionButton = MainState.FabComponent.Visible(id = "", icon = R.drawable.ic_pin),
                )
            },
            onNavigationClick = {},
            onActionButtonClick = {},
            isOffline = true,
            showRetryButton = true,
            onOfflineRetryClick = {},
            hideAllControls = false,
        )
    }
}

@Composable
@ThemePreviews
private fun MainBottomAppBarPreview() {
    ExtendedTheme {
        Box(
            contentAlignment = Alignment.BottomCenter,
        ) {
            MainBottomAppBar(
                state = remember {
                    MainState(
                        title = MainState.TitleComponent.Visible(label = "Sample title"),
                        subtitle = MainState.TitleComponent.Visible(label = "Sample subtitle"),
                        navigation = MainState.NavigationComponent.Visible(id = ""),
                        actionButton = MainState.ActionButtonComponent.Visible(id = "", label = "Action"),
                        bottomAppBar = MainState.BottomAppBarComponent.Visible(
                            id = "",
                            menuItems = stableListOf(MainState.MenuItemComponent.SearchBookmarks),
                            navigationIcon = R.drawable.ic_menu,
                        ),
                        floatingActionButton = MainState.FabComponent.Visible(id = "", icon = R.drawable.ic_pin),
                    )
                },
                onBottomNavClick = {},
                onMenuItemClick = { _, _ -> },
                onFabClick = {},
            )
        }
    }
}
// endregion Previews
