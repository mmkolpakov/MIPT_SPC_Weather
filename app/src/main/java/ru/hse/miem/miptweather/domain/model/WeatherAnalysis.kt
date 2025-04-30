package ru.hse.miem.miptweather.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class WeatherAnalysis(
    val minTemperature: TemperatureExtreme,
    val maxTemperature: TemperatureExtreme,
    val avgTemperature: Float,
    val temperatureAmplitude: Float,
    val temperatureStdDev: Float? = null,
    val rainDays: Int? = null,
    val totalPrecipitation: Float? = null,
    val windyDays: Int? = null,
    val overcastDays: Int? = null,
)

data class TemperatureExtreme(
    val temperature: Float,
    val date: LocalDate,
    val time: LocalDateTime? = null
)