package com.fibelatti.pinboard.features.share

import com.fibelatti.core.functional.Success
import com.fibelatti.core.functional.getOrNull
import com.fibelatti.core.functional.map
import com.fibelatti.core.functional.value
import com.fibelatti.pinboard.features.posts.domain.EditAfterSharing
import com.fibelatti.pinboard.features.posts.domain.model.Post
import com.fibelatti.pinboard.features.posts.domain.usecase.AddPost
import com.fibelatti.pinboard.features.posts.domain.usecase.ExtractUrl
import com.fibelatti.pinboard.features.posts.domain.usecase.ParseUrl
import com.fibelatti.pinboard.features.posts.domain.usecase.RichUrl
import com.fibelatti.pinboard.features.user.domain.UserRepository
import javax.inject.Inject

class ShareReceiver @Inject constructor(
    private val extractUrl: ExtractUrl,
    private val parseUrl: ParseUrl,
    private val addPost: AddPost,
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(bookmarkUrl: String): Result {
        val richUrl = extractUrl(bookmarkUrl)
            .map { extractedUrl -> parseUrl(extractedUrl) }
            .getOrNull() ?: return Result.NotSaved

        return if (userRepository.editAfterSharing is EditAfterSharing.BeforeSaving) {
            editBookmark(richUrl = richUrl)
        } else {
            addBookmark(richUrl = richUrl)
        }
    }

    private fun editBookmark(richUrl: RichUrl): Result {
        val (finalUrl: String, title: String, description: String?) = richUrl
        val newPost = Post(
            url = finalUrl,
            title = title,
            description = description ?: "",
            private = userRepository.defaultPrivate ?: false,
            readLater = userRepository.defaultReadLater ?: false,
            tags = userRepository.defaultTags,
        )

        return Result.Parsed(newPost)
    }

    private suspend fun addBookmark(richUrl: RichUrl): Result {
        val (finalUrl: String, title: String, description: String?) = richUrl
        val params = AddPost.Params(
            url = finalUrl,
            title = title,
            description = description,
            private = userRepository.defaultPrivate,
            readLater = userRepository.defaultReadLater,
            tags = userRepository.defaultTags,
            replace = false,
        )
        val result = addPost(params)

        return when {
            result is Success && userRepository.editAfterSharing is EditAfterSharing.AfterSaving -> {
                Result.Notify(result.value)
            }
            result is Success -> Result.Saved
            else -> Result.NotSaved
        }
    }

    sealed class Result {

        object NotSaved : Result()
        class Parsed(val post: Post) : Result()
        object Saved : Result()
        class Notify(val post: Post) : Result()
    }
}
