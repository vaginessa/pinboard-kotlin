package com.fibelatti.pinboard.core.android.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.fibelatti.pinboard.databinding.LayoutTitleBinding
import com.fibelatti.pinboard.features.appstate.AppStateRepository
import com.fibelatti.pinboard.features.tags.domain.usecase.GetAllTags
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = LayoutTitleBinding.inflate(LayoutInflater.from(context), this, true)

    @Inject
    lateinit var appStateRepository: AppStateRepository

    @Inject
    lateinit var getAllTags: GetAllTags
}
