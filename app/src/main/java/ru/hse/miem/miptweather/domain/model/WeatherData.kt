package ru.hse.miem.miptweather.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.hse.miem.miptweather.domain.model.WeatherCondition // Added import

@Serializable
data class WeatherData(
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val location: Location,
    val dateRange: DateRange,
    val provider: String,
    val timestamp: LocalDateTime
)

@Serializable
data class HourlyWeather(
    val dateTime: LocalDateTime,
    val temperature: Float,
    val feelsLike: Float? = null,
    val humidity: Int? = null,
    val pressure: Float? = null,
    val windSpeed: Float? = null,
    val windDirection: Int? = null,
    val precipitation: Float? = null,
    val weatherCondition: WeatherCondition? = null,
    val uvIndex: Float? = null,
    val cloudiness: Int? = null,
    val visibility: Int? = null
)

@Serializable
data class DailyWeather(
    val date: LocalDate,
    val minTemperature: Float,
    val maxTemperature: Float,
    val avgTemperature: Float? = null,
    val minFeelsLike: Float? = null,
    val maxFeelsLike: Float? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
    val precipitationSum: Float? = null,
    val maxWindSpeed: Float? = null,
    val weatherCondition: WeatherCondition? = null,
    val uvIndexMax: Float? = null,
    val pop: Int? = null
)