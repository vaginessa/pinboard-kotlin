package com.fibelatti.pinboard.features.posts.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.fibelatti.pinboard.features.tags.domain.model.Tag
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class Post(
    val url: String,
    val title: String,
    val description: String,
    val hash: String = "",
    val time: String = "",
    val private: Boolean,
    val readLater: Boolean,
    val tags: List<Tag>? = null,
) : Parcelable
