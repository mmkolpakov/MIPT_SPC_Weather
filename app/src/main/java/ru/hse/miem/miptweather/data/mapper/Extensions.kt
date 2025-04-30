package ru.hse.miem.miptweather.data.mapper

import android.util.Log
import kotlinx.datetime.*
import ru.hse.miem.miptweather.data.dto.*
import ru.hse.miem.miptweather.data.local.LocationEntity
import ru.hse.miem.miptweather.domain.model.*
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

fun LocationEntity.toDomain(): Location = Location(
    id = id,
    latitude = latitude,
    longitude = longitude,
    name = name,
    countryCode = countryCode,
    timezone = timezone
)

fun Location.toEntity(id: Long = 0): LocationEntity = LocationEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    name = name.ifBlank { "Lat: ${String.format(Locale.US, "%.3f", latitude)}, Lon: ${String.format(Locale.US, "%.3f", longitude)}" },
    countryCode = countryCode,
    timezone = timezone.ifEmpty { TimeZone.currentSystemDefault().id }
)


private const val TAG = "WeatherMapper"


fun OpenMeteoResponse.toWeatherData(
    location: Location,
    dateRange: DateRange,
    provider: String,
    timestamp: LocalDateTime
): WeatherData {
    val timezoneId = this.timezone ?: location.timezone.ifEmpty { "UTC" }
    val tz = parseTimeZone(timezoneId)

    val hourlyWeather = mapOpenMeteoHourly(hourly, tz)
    val dailyWeather = mapOpenMeteoDaily(daily, tz)

    val updatedLocation = location.updateTimezoneIfNeeded(this.timezone)

    return WeatherData(
        hourly = hourlyWeather,
        daily = dailyWeather,
        location = updatedLocation,
        dateRange = dateRange,
        provider = provider,
        timestamp = timestamp
    )
}

private fun mapOpenMeteoHourly(hourly: HourlyData?, tz: TimeZone): List<HourlyWeather> {
    if (hourly?.time == null) return emptyList()
    return hourly.time.mapIndexedNotNull { index, timeString ->
        try {
            val dateTime = LocalDateTime.parse(timeString)
            HourlyWeather(
                dateTime = dateTime,
                temperature = hourly.temperature2m?.getOrNull(index) ?: Float.NaN,
                feelsLike = hourly.apparentTemperature?.getOrNull(index),
                humidity = hourly.relativeHumidity2m?.getOrNull(index),
                pressure = hourly.pressureMsl?.getOrNull(index),
                windSpeed = hourly.windspeed10m?.getOrNull(index),
                windDirection = hourly.winddirection10m?.getOrNull(index),
                precipitation = hourly.precipitation?.getOrNull(index),
                weatherCondition = mapOpenMeteoCode(hourly.weathercode?.getOrNull(index)),
                uvIndex = hourly.uvIndex?.getOrNull(index),
                cloudiness = hourly.cloudcover?.getOrNull(index),
                visibility = null
            ).takeIf { !it.temperature.isNaN() }
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Failed to parse OpenMeteo hourly time: $timeString", e)
            null
        }
    }
}

private fun mapOpenMeteoDaily(daily: DailyData?, tz: TimeZone): List<DailyWeather> {
    if (daily?.time == null) return emptyList()
    return daily.time.mapIndexedNotNull { index, dateString ->
        try {
            val date = LocalDate.parse(dateString)
            DailyWeather(
                date = date,
                minTemperature = daily.temperature2mMin?.getOrNull(index) ?: Float.NaN,
                maxTemperature = daily.temperature2mMax?.getOrNull(index) ?: Float.NaN,
                avgTemperature = null,
                minFeelsLike = daily.apparentTemperatureMin?.getOrNull(index),
                maxFeelsLike = daily.apparentTemperatureMax?.getOrNull(index),
                sunrise = daily.sunrise?.getOrNull(index)?.let { safeParseLocalDateTime(it) },
                sunset = daily.sunset?.getOrNull(index)?.let { safeParseLocalDateTime(it) },
                precipitationSum = daily.precipitationSum?.getOrNull(index),
                maxWindSpeed = daily.windspeed10mMax?.getOrNull(index),
                weatherCondition = mapOpenMeteoCode(daily.weathercode?.getOrNull(index)),
                uvIndexMax = daily.uvIndexMax?.getOrNull(index),
                pop = daily.precipitationProbabilityMax?.getOrNull(index)
            ).takeIf { !it.minTemperature.isNaN() && !it.maxTemperature.isNaN() }
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Failed to parse OpenMeteo daily date: $dateString", e)
            null
        }
    }
}


fun OpenWeatherMapResponse.toWeatherData(
    location: Location,
    dateRange: DateRange,
    provider: String,
    timestamp: LocalDateTime
): WeatherData {
    val timezoneId = this.timezone ?: location.timezone.ifEmpty { "UTC" }
    val tz = parseTimeZone(timezoneId)

    val hourlyWeather = mapOpenWeatherMapHourly(current, hourly, tz)
    val dailyWeather = mapOpenWeatherMapDaily(daily, tz)

    val updatedLocation = location.updateTimezoneIfNeeded(this.timezone)

    return WeatherData(
        hourly = hourlyWeather,
        daily = dailyWeather,
        location = updatedLocation,
        dateRange = dateRange,
        provider = provider,
        timestamp = timestamp
    )
}

private fun mapOpenWeatherMapHourly(current: CurrentWeatherDto?, hourlyList: List<HourlyWeatherDto>?, tz: TimeZone): List<HourlyWeather> {
    val currentAsHourly = current?.let { dto ->
        HourlyWeather(
            dateTime = Instant.fromEpochSeconds(dto.dt).toLocalDateTime(tz),
            temperature = dto.temp?.toFloat() ?: Float.NaN,
            feelsLike = dto.feelsLike?.toFloat(),
            humidity = dto.humidity,
            pressure = dto.pressure?.toFloat(),
            windSpeed = dto.windSpeed?.toFloat(),
            windDirection = dto.windDeg,
            precipitation = dto.rain?.get("1h")?.toFloat(),
            weatherCondition = mapOpenWeatherMapCode(dto.weather?.firstOrNull()?.id, dto.dt, dto.sunrise, dto.sunset, tz),
            uvIndex = dto.uvi?.toFloat(),
            cloudiness = dto.clouds,
            visibility = dto.visibility
        ).takeIf { !it.temperature.isNaN() }
    }

    val forecastHourly = hourlyList?.mapNotNull { dto ->
        val dtInstant = Instant.fromEpochSeconds(dto.dt)
        if (currentAsHourly != null && dtInstant == currentAsHourly.dateTime.toInstant(tz)) return@mapNotNull null

        HourlyWeather(
            dateTime = dtInstant.toLocalDateTime(tz),
            temperature = dto.temp?.toFloat() ?: Float.NaN,
            feelsLike = dto.feelsLike?.toFloat(),
            humidity = dto.humidity,
            pressure = dto.pressure?.toFloat(),
            windSpeed = dto.windSpeed?.toFloat(),
            windDirection = dto.windDeg,
            precipitation = dto.rain?.get("1h")?.toFloat(), // Removed snow reference
            weatherCondition = mapOpenWeatherMapCode(dto.weather?.firstOrNull()?.id, dto.dt, null, null, tz),
            uvIndex = dto.uvi?.toFloat(),
            cloudiness = dto.clouds,
            visibility = dto.visibility
        ).takeIf { !it.temperature.isNaN() }
    } ?: emptyList()

    return (listOfNotNull(currentAsHourly) + forecastHourly)
        .distinctBy { it.dateTime }
        .sortedBy { it.dateTime }
}

private fun mapOpenWeatherMapDaily(dailyList: List<DailyWeatherDto>?, tz: TimeZone): List<DailyWeather> {
    if (dailyList == null) return emptyList()
    return dailyList.mapNotNull { dto ->
        val date = Instant.fromEpochSeconds(dto.dt).toLocalDateTime(tz).date
        val sunriseInstant = dto.sunrise?.let { Instant.fromEpochSeconds(it) }
        val sunsetInstant = dto.sunset?.let { Instant.fromEpochSeconds(it) }

        val feelsLikeTemps = listOfNotNull(dto.feelsLike?.day, dto.feelsLike?.night, dto.feelsLike?.eve, dto.feelsLike?.morn)
        val minFeels = feelsLikeTemps.minOrNull()?.toFloat()
        val maxFeels = feelsLikeTemps.maxOrNull()?.toFloat()

        DailyWeather(
            date = date,
            minTemperature = dto.temp?.min?.toFloat() ?: Float.NaN,
            maxTemperature = dto.temp?.max?.toFloat() ?: Float.NaN,
            avgTemperature = dto.temp?.day?.toFloat(),
            minFeelsLike = minFeels,
            maxFeelsLike = maxFeels,
            sunrise = sunriseInstant?.toLocalDateTime(tz),
            sunset = sunsetInstant?.toLocalDateTime(tz),
            precipitationSum = dto.rain?.toFloat() ?: 0f,
            maxWindSpeed = dto.windSpeed?.toFloat(),
            weatherCondition = mapOpenWeatherMapCode(dto.weather?.firstOrNull()?.id, null, null, null, tz),
            uvIndexMax = dto.uvi?.toFloat(),
            pop = dto.pop?.let { (it * 100).roundToInt() }
        ).takeIf { !it.minTemperature.isNaN() && !it.maxTemperature.isNaN() }
    }.sortedBy { it.date }
}


fun WeatherApiResponse.toWeatherData(
    location: Location,
    dateRange: DateRange,
    provider: String,
    timestamp: LocalDateTime
): WeatherData {
    val timezoneId = this.location?.tzId ?: location.timezone.ifEmpty { "UTC" }
    val tz = parseTimeZone(timezoneId)

    val updatedLocation = location.copy(
        name = this.location?.name?.takeIf { it.isNotBlank() } ?: location.name,
        countryCode = this.location?.country?.takeIf { it.isNotBlank() } ?: location.countryCode,
        timezone = timezoneId
    )

    val hourlyWeather = mapWeatherApiHourly(current, forecast, tz)
    val dailyWeather = mapWeatherApiDaily(forecast, tz)

    return WeatherData(
        hourly = hourlyWeather,
        daily = dailyWeather,
        location = updatedLocation,
        dateRange = dateRange,
        provider = provider,
        timestamp = timestamp
    )
}

private fun mapWeatherApiHourly(currentDto: WeatherApiCurrentDto?, forecastDto: ForecastDto?, tz: TimeZone): List<HourlyWeather> {
    val currentAsHourly = currentDto?.lastUpdatedEpoch?.let { epoch ->
        HourlyWeather(
            dateTime = Instant.fromEpochSeconds(epoch).toLocalDateTime(tz),
            temperature = currentDto.tempC?.toFloat() ?: Float.NaN,
            feelsLike = currentDto.feelslikeC?.toFloat(),
            humidity = currentDto.humidity,
            pressure = currentDto.pressureMb?.toFloat(),
            windSpeed = currentDto.windKph?.toFloat()?.div(3.6f),
            windDirection = currentDto.windDegree,
            precipitation = currentDto.precipMm?.toFloat(),
            weatherCondition = mapWeatherApiCode(currentDto.condition?.code, currentDto.isDay == 1),
            uvIndex = currentDto.uv?.toFloat(),
            cloudiness = currentDto.cloud,
            visibility = currentDto.visKm?.toInt()?.times(1000)
        ).takeIf { !it.temperature.isNaN() }
    }

    val forecastHourly = forecastDto?.forecastday?.flatMap { day ->
        day.hour?.mapNotNull { hour ->
            hour.timeEpoch?.let { epoch ->
                val dtInstant = Instant.fromEpochSeconds(epoch)
                if (currentAsHourly != null && dtInstant == currentAsHourly.dateTime.toInstant(tz)) return@mapNotNull null

                HourlyWeather(
                    dateTime = dtInstant.toLocalDateTime(tz),
                    temperature = hour.tempC?.toFloat() ?: Float.NaN,
                    feelsLike = hour.feelslikeC?.toFloat(),
                    humidity = hour.humidity,
                    pressure = hour.pressureMb?.toFloat(),
                    windSpeed = hour.windKph?.toFloat()?.div(3.6f),
                    windDirection = hour.windDegree,
                    precipitation = hour.precipMm?.toFloat(),
                    weatherCondition = mapWeatherApiCode(hour.condition?.code, hour.isDay == 1),
                    uvIndex = hour.uv?.toFloat(),
                    cloudiness = hour.cloud,
                    visibility = hour.visKm?.toInt()?.times(1000)
                ).takeIf { !it.temperature.isNaN() }
            }
        } ?: emptyList()
    } ?: emptyList()

    return (listOfNotNull(currentAsHourly) + forecastHourly)
        .distinctBy { it.dateTime }
        .sortedBy { it.dateTime }
}

private fun mapWeatherApiDaily(forecastDto: ForecastDto?, tz: TimeZone): List<DailyWeather> {
    if (forecastDto?.forecastday == null) return emptyList()
    return forecastDto.forecastday.mapNotNull { dayDto ->
        safeParseLocalDate(dayDto.date)?.let { date ->
            val sunrise = dayDto.astro?.sunrise?.let { convertTimeStringToLocalDateTime(date, it, tz) }
            val sunset = dayDto.astro?.sunset?.let { convertTimeStringToLocalDateTime(date, it, tz) }
            DailyWeather(
                date = date,
                minTemperature = dayDto.day?.mintempC?.toFloat() ?: Float.NaN,
                maxTemperature = dayDto.day?.maxtempC?.toFloat() ?: Float.NaN,
                avgTemperature = dayDto.day?.avgtempC?.toFloat(),
                minFeelsLike = null,
                maxFeelsLike = null,
                sunrise = sunrise,
                sunset = sunset,
                precipitationSum = dayDto.day?.totalprecipMm?.toFloat(),
                maxWindSpeed = dayDto.day?.maxwindKph?.toFloat()?.div(3.6f),
                weatherCondition = mapWeatherApiCode(dayDto.day?.condition?.code, true),
                uvIndexMax = dayDto.day?.uv?.toFloat(),
                pop = dayDto.day?.dailyChanceOfRain ?: dayDto.day?.dailyChanceOfSnow
            ).takeIf { !it.minTemperature.isNaN() && !it.maxTemperature.isNaN() }
        } ?: run {
            Log.e(TAG, "Failed to parse WeatherAPI daily date: ${dayDto.date}")
            null
        }
    }.sortedBy { it.date }
}


private fun parseTimeZone(zoneId: String?): TimeZone {
    return try {
        if (zoneId.isNullOrBlank()) TimeZone.UTC else TimeZone.of(zoneId)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse TimeZone ID '$zoneId', falling back to UTC.", e)
        TimeZone.UTC
    }
}

private fun Location.updateTimezoneIfNeeded(timezoneIdFromApi: String?): Location {
    return if (!timezoneIdFromApi.isNullOrBlank() && this.timezone != timezoneIdFromApi) {
        this.copy(timezone = timezoneIdFromApi)
    } else {
        this
    }
}

private fun safeParseLocalDateTime(dateTimeString: String?): LocalDateTime? {
    if (dateTimeString.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(dateTimeString.replace(" ", "T"))
    } catch (e: Exception) {
        Log.w(TAG, "Could not parse LocalDateTime: $dateTimeString")
        null
    }
}

private fun safeParseLocalDate(dateString: String?): LocalDate? {
    if (dateString.isNullOrBlank()) return null
    return try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        Log.w(TAG, "Could not parse LocalDate: $dateString")
        null
    }
}


private fun convertTimeStringToLocalDateTime(date: LocalDate, timeString: String?, timeZone: TimeZone): LocalDateTime? {
    if (timeString.isNullOrBlank()) return null
    return try {
        val trimmedTime = timeString.trim()
        val parts = trimmedTime.split(":", " ")
        if (parts.size < 2) return null

        val hourPart = parts[0].toIntOrNull() ?: return null
        val minutePart = parts[1].toIntOrNull() ?: return null
        var hour = hourPart

        if (parts.size >= 3 && parts.last().equals("PM", ignoreCase = true) && hour < 12) hour += 12
        else if (parts.size >= 3 && parts.last().equals("AM", ignoreCase = true) && hour == 12) hour = 0

        LocalDateTime(date, LocalTime(hour % 24, minutePart))
    } catch (e: Exception) {
        Log.w(TAG, "Could not convert time string '$timeString' for date '$date'")
        null
    }
}


private fun mapOpenMeteoCode(code: Int?): WeatherCondition = when (code) {
    0 -> WeatherCondition.CLEAR_SKY_DAY
    1 -> WeatherCondition.FEW_CLOUDS_DAY
    2 -> WeatherCondition.SCATTERED_CLOUDS
    3 -> WeatherCondition.OVERCAST_CLOUDS
    45, 48 -> WeatherCondition.FOG
    51, 53, 55 -> WeatherCondition.LIGHT_DRIZZLE
    56, 57 -> WeatherCondition.FREEZING_RAIN
    61 -> WeatherCondition.LIGHT_RAIN
    63 -> WeatherCondition.MODERATE_RAIN
    65 -> WeatherCondition.HEAVY_RAIN
    66, 67 -> WeatherCondition.FREEZING_RAIN
    71 -> WeatherCondition.LIGHT_SNOW
    73 -> WeatherCondition.SNOW
    75 -> WeatherCondition.HEAVY_SNOW
    77 -> WeatherCondition.SNOW
    80 -> WeatherCondition.LIGHT_SHOWER_RAIN
    81 -> WeatherCondition.SHOWER_RAIN
    82 -> WeatherCondition.HEAVY_SHOWER_RAIN
    85 -> WeatherCondition.LIGHT_SHOWER_SNOW
    86 -> WeatherCondition.HEAVY_SHOWER_SNOW
    95 -> WeatherCondition.THUNDERSTORM
    96, 99 -> WeatherCondition.THUNDERSTORM_HEAVY_RAIN
    else -> WeatherCondition.UNKNOWN
}

private fun mapOpenWeatherMapCode(code: Int?, dt: Long?, sunrise: Long?, sunset: Long?, timeZone: TimeZone): WeatherCondition {
    val isDay = dt?.let { isDayTime(it, sunrise, sunset) }

    return when (code) {
        in 200..202 -> WeatherCondition.THUNDERSTORM_RAIN
        in 210..211 -> WeatherCondition.THUNDERSTORM
        212 -> WeatherCondition.HEAVY_THUNDERSTORM
        in 221..232 -> WeatherCondition.THUNDERSTORM_RAIN
        in 300..301 -> WeatherCondition.LIGHT_DRIZZLE
        302, 311, 313, 314, 321 -> WeatherCondition.DRIZZLE
        310 -> WeatherCondition.LIGHT_DRIZZLE_RAIN
        312 -> WeatherCondition.HEAVY_DRIZZLE_RAIN
        500 -> WeatherCondition.LIGHT_RAIN
        501 -> WeatherCondition.MODERATE_RAIN
        502 -> WeatherCondition.HEAVY_RAIN
        503 -> WeatherCondition.VERY_HEAVY_RAIN
        504 -> WeatherCondition.EXTREME_RAIN
        511 -> WeatherCondition.FREEZING_RAIN
        520 -> WeatherCondition.LIGHT_SHOWER_RAIN
        521 -> WeatherCondition.SHOWER_RAIN
        522 -> WeatherCondition.HEAVY_SHOWER_RAIN
        531 -> WeatherCondition.RAGGED_SHOWER_RAIN
        600 -> WeatherCondition.LIGHT_SNOW
        601 -> WeatherCondition.SNOW
        602 -> WeatherCondition.HEAVY_SNOW
        611, 612, 613 -> WeatherCondition.SLEET
        615 -> WeatherCondition.LIGHT_RAIN_SNOW
        616 -> WeatherCondition.RAIN_SNOW
        620 -> WeatherCondition.LIGHT_SHOWER_SNOW
        621 -> WeatherCondition.SHOWER_SNOW
        622 -> WeatherCondition.HEAVY_SHOWER_SNOW
        701 -> WeatherCondition.MIST
        711 -> WeatherCondition.SMOKE
        721 -> WeatherCondition.HAZE
        731 -> WeatherCondition.DUST_WHIRLS
        741 -> WeatherCondition.FOG
        751 -> WeatherCondition.SAND
        761 -> WeatherCondition.DUST
        762 -> WeatherCondition.VOLCANIC_ASH
        771 -> WeatherCondition.SQUALLS
        781 -> WeatherCondition.TORNADO
        800 -> if (isDay == true) WeatherCondition.CLEAR_SKY_DAY else WeatherCondition.CLEAR_SKY_NIGHT
        801 -> if (isDay == true) WeatherCondition.FEW_CLOUDS_DAY else WeatherCondition.FEW_CLOUDS_NIGHT
        802 -> WeatherCondition.SCATTERED_CLOUDS
        803 -> WeatherCondition.BROKEN_CLOUDS
        804 -> WeatherCondition.OVERCAST_CLOUDS
        else -> WeatherCondition.UNKNOWN
    }
}

private fun mapWeatherApiCode(code: Int?, isDay: Boolean?): WeatherCondition {
    val day = isDay ?: true

    return when (code) {
        1000 -> if (day) WeatherCondition.CLEAR_SKY_DAY else WeatherCondition.CLEAR_SKY_NIGHT
        1003 -> if (day) WeatherCondition.FEW_CLOUDS_DAY else WeatherCondition.FEW_CLOUDS_NIGHT
        1006 -> WeatherCondition.SCATTERED_CLOUDS
        1009 -> WeatherCondition.OVERCAST_CLOUDS
        1030 -> WeatherCondition.MIST
        1063 -> WeatherCondition.LIGHT_RAIN
        1066 -> WeatherCondition.LIGHT_SNOW
        1069 -> WeatherCondition.SLEET
        1072 -> WeatherCondition.LIGHT_DRIZZLE
        1087 -> WeatherCondition.THUNDERSTORM
        1114 -> WeatherCondition.SNOW
        1117 -> WeatherCondition.HEAVY_SNOW
        1135 -> WeatherCondition.FOG
        1147 -> WeatherCondition.FOG
        1150, 1153, 1168 -> WeatherCondition.LIGHT_DRIZZLE
        1171 -> WeatherCondition.HEAVY_DRIZZLE
        1180, 1183 -> WeatherCondition.LIGHT_RAIN
        1186, 1189 -> WeatherCondition.MODERATE_RAIN
        1192, 1195 -> WeatherCondition.HEAVY_RAIN
        1198, 1201 -> WeatherCondition.FREEZING_RAIN
        1204, 1207, 1249, 1252, 1261, 1264, 1237 -> WeatherCondition.SLEET
        1210, 1213, 1255 -> WeatherCondition.LIGHT_SNOW
        1216, 1219, 1258 -> WeatherCondition.SNOW
        1222, 1225 -> WeatherCondition.HEAVY_SNOW
        1240 -> WeatherCondition.LIGHT_SHOWER_RAIN
        1243 -> WeatherCondition.SHOWER_RAIN
        1246 -> WeatherCondition.HEAVY_SHOWER_RAIN
        1273 -> WeatherCondition.THUNDERSTORM_LIGHT_RAIN
        1276 -> WeatherCondition.THUNDERSTORM_HEAVY_RAIN
        1279 -> WeatherCondition.THUNDERSTORM_LIGHT_RAIN
        1282 -> WeatherCondition.THUNDERSTORM_HEAVY_RAIN
        else -> WeatherCondition.UNKNOWN
    }
}


private fun isDayTime(dtEpochSeconds: Long, sunriseEpochSeconds: Long?, sunsetEpochSeconds: Long?): Boolean? {
    if (sunriseEpochSeconds == null || sunsetEpochSeconds == null) return null
    if (sunriseEpochSeconds > sunsetEpochSeconds) return null
    return dtEpochSeconds in sunriseEpochSeconds..sunsetEpochSeconds
}