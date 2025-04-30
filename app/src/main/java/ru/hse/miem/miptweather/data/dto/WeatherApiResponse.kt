package ru.hse.miem.miptweather.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiResponse(
    val location: LocationDto? = null,
    val current: WeatherApiCurrentDto? = null,
    val forecast: ForecastDto? = null
)

@Serializable
data class LocationDto(
    val name: String? = null,
    val region: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("tz_id") val tzId: String? = null,
    @SerialName("localtime_epoch") val localtimeEpoch: Long? = null,
    val localtime: String? = null
)

@Serializable
data class WeatherApiCurrentDto(
    @SerialName("last_updated_epoch") val lastUpdatedEpoch: Long? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("temp_c") val tempC: Double? = null,
    @SerialName("temp_f") val tempF: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val condition: ConditionDto? = null,
    @SerialName("wind_mph") val windMph: Double? = null,
    @SerialName("wind_kph") val windKph: Double? = null,
    @SerialName("wind_degree") val windDegree: Int? = null,
    @SerialName("wind_dir") val windDir: String? = null,
    @SerialName("pressure_mb") val pressureMb: Double? = null,
    @SerialName("pressure_in") val pressureIn: Double? = null,
    @SerialName("precip_mm") val precipMm: Double? = null,
    @SerialName("precip_in") val precipIn: Double? = null,
    val humidity: Int? = null,
    val cloud: Int? = null,
    @SerialName("feelslike_c") val feelslikeC: Double? = null,
    @SerialName("feelslike_f") val feelslikeF: Double? = null,
    @SerialName("vis_km") val visKm: Double? = null,
    @SerialName("vis_miles") val visMiles: Double? = null,
    val uv: Double? = null,
    @SerialName("gust_mph") val gustMph: Double? = null,
    @SerialName("gust_kph") val gustKph: Double? = null
)

@Serializable
data class ConditionDto(
    val text: String? = null,
    val icon: String? = null,
    val code: Int? = null
)

@Serializable
data class ForecastDto(
    val forecastday: List<ForecastDayDto>? = null
)

@Serializable
data class ForecastDayDto(
    val date: String? = null,
    @SerialName("date_epoch") val dateEpoch: Long? = null,
    val day: DayDto? = null,
    val astro: AstroDto? = null,
    val hour: List<HourDto>? = null
)

@Serializable
data class DayDto(
    @SerialName("maxtemp_c") val maxtempC: Double? = null,
    @SerialName("maxtemp_f") val maxtempF: Double? = null,
    @SerialName("mintemp_c") val mintempC: Double? = null,
    @SerialName("mintemp_f") val mintempF: Double? = null,
    @SerialName("avgtemp_c") val avgtempC: Double? = null,
    @SerialName("avgtemp_f") val avgtempF: Double? = null,
    @SerialName("maxwind_mph") val maxwindMph: Double? = null,
    @SerialName("maxwind_kph") val maxwindKph: Double? = null,
    @SerialName("totalprecip_mm") val totalprecipMm: Double? = null,
    @SerialName("totalprecip_in") val totalprecipIn: Double? = null,
    @SerialName("totalsnow_cm") val totalsnowCm: Double? = null,
    @SerialName("avgvis_km") val avgvisKm: Double? = null,
    @SerialName("avgvis_miles") val avgvisMiles: Double? = null,
    @SerialName("avghumidity") val avghumidity: Int? = null,
    @SerialName("daily_will_it_rain") val dailyWillItRain: Int? = null,
    @SerialName("daily_chance_of_rain") val dailyChanceOfRain: Int? = null,
    @SerialName("daily_will_it_snow") val dailyWillItSnow: Int? = null,
    @SerialName("daily_chance_of_snow") val dailyChanceOfSnow: Int? = null,
    val condition: ConditionDto? = null,
    val uv: Double? = null
)

@Serializable
data class AstroDto(
    val sunrise: String? = null,
    val sunset: String? = null,
    val moonrise: String? = null,
    val moonset: String? = null,
    @SerialName("moon_phase") val moonPhase: String? = null,
    @SerialName("moon_illumination") val moonIllumination: String? = null,
    @SerialName("is_moon_up") val isMoonUp: Int? = null,
    @SerialName("is_sun_up") val isSunUp: Int? = null
)

@Serializable
data class HourDto(
    @SerialName("time_epoch") val timeEpoch: Long? = null,
    val time: String? = null,
    @SerialName("temp_c") val tempC: Double? = null,
    @SerialName("temp_f") val tempF: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val condition: ConditionDto? = null,
    @SerialName("wind_mph") val windMph: Double? = null,
    @SerialName("wind_kph") val windKph: Double? = null,
    @SerialName("wind_degree") val windDegree: Int? = null,
    @SerialName("wind_dir") val windDir: String? = null,
    @SerialName("pressure_mb") val pressureMb: Double? = null,
    @SerialName("pressure_in") val pressureIn: Double? = null,
    @SerialName("precip_mm") val precipMm: Double? = null,
    @SerialName("precip_in") val precipIn: Double? = null,
    val humidity: Int? = null,
    val cloud: Int? = null,
    @SerialName("feelslike_c") val feelslikeC: Double? = null,
    @SerialName("feelslike_f") val feelslikeF: Double? = null,
    @SerialName("windchill_c") val windchillC: Double? = null,
    @SerialName("windchill_f") val windchillF: Double? = null,
    @SerialName("heatindex_c") val heatindexC: Double? = null,
    @SerialName("heatindex_f") val heatindexF: Double? = null,
    @SerialName("dewpoint_c") val dewpointC: Double? = null,
    @SerialName("dewpoint_f") val dewpointF: Double? = null,
    @SerialName("will_it_rain") val willItRain: Int? = null,
    @SerialName("chance_of_rain") val chanceOfRain: Int? = null,
    @SerialName("will_it_snow") val willItSnow: Int? = null,
    @SerialName("chance_of_snow") val chanceOfSnow: Int? = null,
    @SerialName("vis_km") val visKm: Double? = null,
    @SerialName("vis_miles") val visMiles: Double? = null,
    @SerialName("gust_mph") val gustMph: Double? = null,
    @SerialName("gust_kph") val gustKph: Double? = null,
    val uv: Double? = null
)