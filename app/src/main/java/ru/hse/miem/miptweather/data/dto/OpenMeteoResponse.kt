package ru.hse.miem.miptweather.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val hourly: HourlyData? = null,
    val daily: DailyData? = null
)

@Serializable
data class HourlyData(
    val time: List<String>? = null,
    @SerialName("temperature_2m") val temperature2m: List<Float>? = null,
    @SerialName("relativehumidity_2m") val relativeHumidity2m: List<Int>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Float>? = null, // Added
    val precipitation: List<Float>? = null,
    val weathercode: List<Int>? = null,
    @SerialName("pressure_msl") val pressureMsl: List<Float>? = null,
    @SerialName("windspeed_10m") val windspeed10m: List<Float>? = null,
    @SerialName("winddirection_10m") val winddirection10m: List<Int>? = null,
    @SerialName("uv_index") val uvIndex: List<Float>? = null,
    @SerialName("cloudcover") val cloudcover: List<Int>? = null // Added
)

@Serializable
data class DailyData(
    val time: List<String>? = null,
    val weathercode: List<Int>? = null,
    @SerialName("temperature_2m_max") val temperature2mMax: List<Float>? = null,
    @SerialName("temperature_2m_min") val temperature2mMin: List<Float>? = null,
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Float>? = null, // Added
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Float>? = null, // Added
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    @SerialName("uv_index_max") val uvIndexMax: List<Float>? = null,
    @SerialName("precipitation_sum") val precipitationSum: List<Float>? = null,
    @SerialName("windspeed_10m_max") val windspeed10mMax: List<Float>? = null,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null // Added (usually Int %)
)