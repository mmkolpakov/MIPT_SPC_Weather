package ru.hse.miem.miptweather.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenWeatherMapResponse(
    val lat: Double? = null,
    val lon: Double? = null,
    val timezone: String? = null,
    val current: CurrentWeatherDto? = null,
    val hourly: List<HourlyWeatherDto>? = null,
    val daily: List<DailyWeatherDto>? = null
)

@Serializable
data class CurrentWeatherDto(
    val dt: Long,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val temp: Double? = null,
    @SerialName("feels_like") val feelsLike: Double? = null,
    val pressure: Int? = null,
    val humidity: Int? = null,
    @SerialName("dew_point") val dewPoint: Double? = null,
    val uvi: Double? = null,
    val clouds: Int? = null,
    val visibility: Int? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("wind_deg") val windDeg: Int? = null,
    val weather: List<WeatherDto>? = null,
    val rain: Map<String, Double>? = null
)

@Serializable
data class HourlyWeatherDto(
    val dt: Long,
    val temp: Double? = null,
    @SerialName("feels_like") val feelsLike: Double? = null,
    val pressure: Int? = null,
    val humidity: Int? = null,
    @SerialName("dew_point") val dewPoint: Double? = null,
    val uvi: Double? = null,
    val clouds: Int? = null,
    val visibility: Int? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("wind_deg") val windDeg: Int? = null,
    val weather: List<WeatherDto>? = null,
    val pop: Double? = null,
    val rain: Map<String, Double>? = null
)

@Serializable
data class DailyWeatherDto(
    val dt: Long,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val temp: TempDto? = null,
    @SerialName("feels_like") val feelsLike: FeelsLikeDto? = null,
    val pressure: Int? = null,
    val humidity: Int? = null,
    @SerialName("dew_point") val dewPoint: Double? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("wind_deg") val windDeg: Int? = null,
    val weather: List<WeatherDto>? = null,
    val clouds: Int? = null,
    val pop: Double? = null,
    val rain: Double? = null,
    val uvi: Double? = null
)

@Serializable
data class TempDto(
    val day: Double? = null,
    val min: Double? = null,
    val max: Double? = null,
    val night: Double? = null,
    val eve: Double? = null,
    val morn: Double? = null
)

@Serializable
data class FeelsLikeDto(
    val day: Double? = null,
    val night: Double? = null,
    val eve: Double? = null,
    val morn: Double? = null
)

@Serializable
data class WeatherDto(
    val id: Int? = null,
    val main: String? = null,
    val description: String? = null,
    val icon: String? = null
)