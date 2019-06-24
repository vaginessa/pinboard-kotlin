package com.fibelatti.pinboard.features.appstate

import android.net.ConnectivityManager
import com.fibelatti.core.provider.ResourceProvider
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.core.extension.isConnected
import javax.inject.Inject

class NavigationActionHandler @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val connectivityManager: ConnectivityManager?
) {

    fun runAction(action: NavigationAction, currentContent: Content): Content {
        return when (action) {
            is NavigateBack -> navigateBack(currentContent)
            is ViewCategory -> viewCategory(action)
            is ViewPost -> viewPost(action, currentContent)
            is ViewSearch -> viewSearch(currentContent)
            is AddPost -> viewAddPost(currentContent)
            is ViewTags -> viewTags(currentContent)
        }
    }

    private fun navigateBack(currentContent: Content): Content {
        return if (currentContent is ContentWithHistory) {
            currentContent.previousContent
        } else {
            currentContent
        }
    }

    private fun viewCategory(action: ViewCategory): Content {
        return PostList(
            category = action,
            title = when (action) {
                All -> resourceProvider.getString(R.string.posts_title_all)
                Recent -> resourceProvider.getString(R.string.posts_title_recent)
                Public -> resourceProvider.getString(R.string.posts_title_public)
                Private -> resourceProvider.getString(R.string.posts_title_private)
                Unread -> resourceProvider.getString(R.string.posts_title_unread)
                Untagged -> resourceProvider.getString(R.string.posts_title_untagged)
            },
            posts = emptyList(),
            sortType = NewestFirst,
            searchParameters = SearchParameters(),
            shouldLoad = connectivityManager.isConnected(),
            isConnected = connectivityManager.isConnected()
        )
    }

    private fun viewPost(action: ViewPost, currentContent: Content): Content {
        return if (currentContent is PostList) {
            PostDetail(action.post, previousContent = currentContent)
        } else {
            currentContent
        }
    }

    private fun viewSearch(currentContent: Content): Content {
        return if (currentContent is PostList) {
            SearchView(currentContent.searchParameters, shouldLoadTags = true, previousContent = currentContent)
        } else {
            currentContent
        }
    }

    private fun viewAddPost(currentContent: Content): Content {
        return if (currentContent is PostList) {
            AddPostView(previousContent = currentContent)
        } else {
            currentContent
        }
    }

    private fun viewTags(currentContent: Content): Content {
        return if (currentContent is PostList) {
            TagList(
                tags = emptyList(),
                shouldLoad = connectivityManager.isConnected(),
                isConnected = connectivityManager.isConnected(),
                previousContent = currentContent
            )
        } else {
            currentContent
        }
    }
}