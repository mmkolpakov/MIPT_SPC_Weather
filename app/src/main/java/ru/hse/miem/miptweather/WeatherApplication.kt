package ru.hse.miem.miptweather

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager // Импортируем WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import ru.hse.miem.miptweather.service.WeatherProviderService
import ru.hse.miem.miptweather.service.WorkSchedulerService
import javax.inject.Inject

@HiltAndroidApp
class WeatherApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var weatherProviderService: WeatherProviderService
    @Inject lateinit var workSchedulerService: WorkSchedulerService
    @Inject lateinit var workManager: WorkManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        initializeComponents()
    }

    private fun initializeComponents() {
        Log.d(TAG, "Initializing application components...")

        weatherProviderService.registerAllProvidersIfNeeded()

        applicationScope.launch {
            try {
                workSchedulerService.scheduleAllTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule background tasks", e)
            }
        }

        Log.d(TAG, "Application components initialization finished.")
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel("Application terminated")
        Log.i(TAG, "Application terminated, scope cancelled.")
    }

    companion object {
        private const val TAG = "WeatherApplication"
    }
}