package com.fibelatti.pinboard.features.share

import android.content.Intent
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fibelatti.pinboard.core.android.base.BaseActivity

class ShareReceiverActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.checkForExtraText(::enqueueWork)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun Intent.checkForExtraText(onExtraTextFound: (String) -> Unit) {
        takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.let(onExtraTextFound)
    }

    private fun enqueueWork(bookmarkUrl: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<ShareReceiverWorker>()
            .setConstraints(constraints)
            .setInputData(ShareReceiverWorker.createData(bookmarkUrl))
            .build()
        WorkManager.getInstance(application).enqueue(work)
    }
}
