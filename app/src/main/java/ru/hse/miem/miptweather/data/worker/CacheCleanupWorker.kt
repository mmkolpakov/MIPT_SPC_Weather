package ru.hse.miem.miptweather.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import ru.hse.miem.miptweather.data.local.WeatherDao

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherDao: WeatherDao
) : CoroutineWorker(context, params) {

    private companion object {
        const val TAG = "CacheCleanupWorker"
        const val CACHE_RETENTION_DAYS = 30
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting cache cleanup work...")
        try {
            val systemTimeZone = TimeZone.currentSystemDefault()
            val today = Clock.System.todayIn(systemTimeZone)
            val olderThanDate = today.minus(CACHE_RETENTION_DAYS, DateTimeUnit.DAY)
            val olderThanDateTime = LocalDateTime(olderThanDate, LocalTime(0, 0))

            Log.d(TAG, "Deleting weather data older than: $olderThanDateTime")
            weatherDao.deleteOldData(olderThanDateTime)
            Log.i(TAG, "Cache cleanup finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cache cleanup failed", e)
            Result.retry()
        }
    }
}