package ru.hse.miem.miptweather.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import ru.hse.miem.miptweather.BuildConfig
import ru.hse.miem.miptweather.data.local.LocationDao
import ru.hse.miem.miptweather.data.local.WeatherDao
import ru.hse.miem.miptweather.data.local.WeatherDatabase
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val TAG = "AppModule"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideWeatherDatabase(
        @ApplicationContext context: Context
    ): WeatherDatabase = Room.databaseBuilder(
        context,
        WeatherDatabase::class.java,
        "weather_database"
    )
        .fallbackToDestructiveMigration(true)
        .build()

    @Provides
    @Singleton
    fun provideWeatherDao(database: WeatherDatabase): WeatherDao = database.weatherDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: WeatherDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    @Named("OpenWeatherMapApiKey")
    fun provideOpenWeatherMapApiKey(): String = BuildConfig.OPENWEATHERMAP_API_KEY.also {
        if (it.isBlank()) Log.w(TAG, "OpenWeatherMap API Key is blank!")
    }

    @Provides
    @Singleton
    @Named("WeatherApiKey")
    fun provideWeatherApiKey(): String = BuildConfig.WEATHERAPI_COM_API_KEY.also {
        if (it.isBlank()) Log.w(TAG, "WeatherAPI.com API Key is blank!")
    }
}