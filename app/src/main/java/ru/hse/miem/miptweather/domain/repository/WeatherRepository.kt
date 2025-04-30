package ru.hse.miem.miptweather.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.util.Result

interface WeatherRepository {
    fun getWeatherData(
        location: Location,
        dateRange: DateRange,
        forceRefresh: Boolean = false
    ): Flow<Result<WeatherData>>

    fun getAvailableProviders(): Flow<List<String>>

    suspend fun setPreferredProvider(providerId: String)

    fun getPreferredProvider(): Flow<String>
}