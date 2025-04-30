package ru.hse.miem.miptweather.data.api

import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherCapability
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.util.Result

interface WeatherProvider {
    val id: String
    val name: String
    val capabilities: Set<WeatherCapability>
    val priority: Int

    suspend fun getWeatherData(
        location: Location,
        dateRange: DateRange
    ): Result<WeatherData>
}