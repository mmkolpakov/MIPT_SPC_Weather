package ru.hse.miem.miptweather.service

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.hse.miem.miptweather.data.worker.CacheCleanupWorker
import ru.hse.miem.miptweather.presentation.widget.WeatherWidget
import ru.hse.miem.miptweather.presentation.widget.WeatherWidgetWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkSchedulerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    companion object {
        const val WIDGET_UPDATE_WORK_NAME = "weather_widget_periodic_update"
        const val CACHE_CLEANUP_WORK_NAME = "weather_cache_periodic_cleanup"
        private val WIDGET_UPDATE_INTERVAL = 1L to TimeUnit.HOURS
        private val CACHE_CLEANUP_INTERVAL = 1L to TimeUnit.DAYS
    }

    suspend fun scheduleAllTasks() {
        scheduleWidgetUpdates()
        scheduleCacheCleanup()
    }

    private suspend fun scheduleWidgetUpdates() {
        val widgetManager = GlanceAppWidgetManager(context)
        val widgetIds = try {
            widgetManager.getGlanceIds(WeatherWidget::class.java)
        } catch (e: Exception) {
            emptyList()
        }

        if (widgetIds.isEmpty()) {
            workManager.cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val (repeatInterval, timeUnit) = WIDGET_UPDATE_INTERVAL
        val updateRequest = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(repeatInterval, timeUnit)
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    private fun scheduleCacheCleanup() {
        val (repeatInterval, timeUnit) = CACHE_CLEANUP_INTERVAL
        val cleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(repeatInterval, timeUnit)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CACHE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}