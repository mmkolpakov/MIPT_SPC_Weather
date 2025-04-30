package ru.hse.miem.miptweather.presentation.widget

import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class UpdateWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        val updateRequest = OneTimeWorkRequestBuilder<WeatherWidgetWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_manual_update_$glanceId",
            ExistingWorkPolicy.REPLACE,
            updateRequest
        )
    }
}