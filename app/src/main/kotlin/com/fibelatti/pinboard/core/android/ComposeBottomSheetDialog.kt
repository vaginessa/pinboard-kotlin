package com.fibelatti.pinboard.core.android

import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.core.extension.setThemedContent
import com.fibelatti.pinboard.core.extension.setViewTreeOwners
import com.fibelatti.pinboard.features.user.domain.UserRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ComposeBottomSheetDialog(
    context: Context,
    content: @Composable BottomSheetDialog.() -> Unit,
) : BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog) {

    init {
        val entryPoint = EntryPointAccessors.fromApplication<DialogEntryPoint>(context.applicationContext)

        if (entryPoint.userRepository().disableScreenshots) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        behavior.peekHeight = 1200.dp.value.toInt()
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        setViewTreeOwners()

        setContentView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setThemedContent {
                    content()
                }
            },
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DialogEntryPoint {

    fun userRepository(): UserRepository
}
