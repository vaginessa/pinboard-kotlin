package com.fibelatti.pinboard.features.appstate

import androidx.lifecycle.viewModelScope
import com.fibelatti.pinboard.core.android.base.BaseViewModel
import com.fibelatti.pinboard.core.network.UnauthorizedInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    unauthorizedInterceptor: UnauthorizedInterceptor,
) : BaseViewModel() {

    val content: Flow<Content> = appStateRepository.content

    val postListContent: Flow<PostListContent> get() = filteredContent()
    val postDetailContent: Flow<PostDetailContent> get() = filteredContent()
    val addPostContent: Flow<AddPostContent> get() = filteredContent()
    val editPostContent: Flow<EditPostContent> get() = filteredContent()
    val searchContent: Flow<SearchContent> get() = filteredContent()
    val tagListContent: Flow<TagListContent> get() = filteredContent()
    val noteListContent: Flow<NoteListContent> get() = filteredContent()
    val noteDetailContent: Flow<NoteDetailContent> get() = filteredContent()
    val popularPostsContent: Flow<PopularPostsContent> get() = filteredContent()
    val popularPostDetailContent: Flow<PopularPostDetailContent> get() = filteredContent()
    val userPreferencesContent: Flow<UserPreferencesContent> get() = filteredContent()

    init {
        unauthorizedInterceptor.unauthorized
            .onEach { appStateRepository.runAction(UserUnauthorized) }
            .launchIn(viewModelScope)
    }

    fun reset() {
        launch { appStateRepository.reset() }
    }

    fun runAction(action: Action) {
        launch { appStateRepository.runAction(action) }
    }

    private inline fun <reified T> filteredContent() = content.mapNotNull { it as? T }
}
