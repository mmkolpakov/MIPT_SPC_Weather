package ru.hse.miem.miptweather.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import ru.hse.miem.miptweather.presentation.widget.WeatherWidget.Companion.KEY_ERROR_MESSAGE
import ru.hse.miem.miptweather.presentation.widget.WeatherWidget.Companion.KEY_WEATHER_DATA
import androidx.work.ListenableWorker.Result as WorkerResult

@HiltWorker
class WeatherWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val json: Json
) : CoroutineWorker(context, params) {

    private companion object { const val TAG = "WeatherWidgetWorker" }

    override suspend fun doWork(): WorkerResult = withContext(Dispatchers.IO) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = try {
            manager.getGlanceIds(WeatherWidget::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get glance IDs", e)
            return@withContext WorkerResult.failure()
        }

        if (glanceIds.isEmpty()) {
            Log.i(TAG, "No active weather widgets found. Stopping worker.")
            return@withContext WorkerResult.success()
        }
        Log.i(TAG, "Starting update for ${glanceIds.size} widgets.")

        val location = locationRepository.getSavedLocations().firstOrNull()?.firstOrNull()

        if (location == null) {
            Log.w(TAG, "No saved locations found. Cannot update widgets with weather data.")
            glanceIds.forEach { updateWidgetWithError(it, "Нет сохраненных локаций") }
            return@withContext WorkerResult.success()
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val endDate = today.plus(3, DateTimeUnit.DAY)
        val dateRange = DateRange(today, endDate)

        Log.d(TAG, "Fetching weather for widget location: ${location.getDisplayName()}, range=$dateRange")

        val weatherResult = weatherRepository.getWeatherData(location, dateRange, forceRefresh = true)
            .filter { it !is ru.hse.miem.miptweather.domain.util.Result.Loading }
            .first()

        try {
            when (weatherResult) {
                is ru.hse.miem.miptweather.domain.util.Result.Success -> {
                    Log.i(TAG, "Successfully fetched weather data for widgets.")
                    val weatherData = weatherResult.data
                    val weatherDataJson = json.encodeToString(WeatherData.serializer(), weatherData)
                    glanceIds.forEach { updateWidgetWithData(it, weatherDataJson) }
                    WorkerResult.success()
                }
                is ru.hse.miem.miptweather.domain.util.Result.Error -> {
                    val errorMsg = weatherResult.exception.localizedMessage ?: "Неизвестная ошибка сети"
                    Log.e(TAG, "Error fetching weather data for widgets: $errorMsg", weatherResult.exception)
                    glanceIds.forEach { updateWidgetWithError(it, errorMsg.take(100)) }
                    WorkerResult.retry()
                }
                is ru.hse.miem.miptweather.domain.util.Result.Loading -> {
                    Log.wtf(TAG, "Received Loading state after filtering!")
                    WorkerResult.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed unexpectedly", e)
            val errorMsg = e.localizedMessage ?: "Внутренняя ошибка"
            glanceIds.forEach { updateWidgetWithError(it, errorMsg.take(100)) }
            WorkerResult.failure()
        }
    }

    private suspend fun updateWidgetWithData(glanceId: GlanceId, weatherDataJson: String) {
        try {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[KEY_WEATHER_DATA] = weatherDataJson
                prefs.remove(KEY_ERROR_MESSAGE)
            }
            WeatherWidget().update(context, glanceId)
            Log.d(TAG, "Widget $glanceId updated with data.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $glanceId state with data", e)
        }
    }

    private suspend fun updateWidgetWithError(glanceId: GlanceId, errorMessage: String) {
        try {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[KEY_ERROR_MESSAGE] = errorMessage
                prefs.remove(KEY_WEATHER_DATA)
            }
            WeatherWidget().update(context, glanceId)
            Log.w(TAG, "Widget $glanceId updated with error: $errorMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $glanceId state with error", e)
        }
    }
}