package ru.hse.miem.miptweather.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.WeatherCondition
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

private val LocalTimeFormatter = staticCompositionLocalOf { timeFormatter }
private val LocalDayMonthFormatter = staticCompositionLocalOf { dayMonthFormatter }
private val LocalDayOfWeekFormatter = staticCompositionLocalOf { dayOfWeekFormatter }

@Composable
@ReadOnlyComposable
fun formatTemperature(
    temperature: Float?,
    unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    showUnit: Boolean = true
): String {
    if (temperature == null || temperature.isNaN()) return stringResource(R.string.not_available_short)

    val tempValue = temperature.roundToInt()
    return if (!showUnit) {
        stringResource(R.string.temperature_value_only, tempValue)
    } else {
        when (unit) {
            TemperatureUnit.CELSIUS -> stringResource(
                R.string.temperature_format_celsius,
                tempValue
            )

            TemperatureUnit.FAHRENHEIT -> stringResource(
                R.string.temperature_format_fahrenheit,
                tempValue
            )
        }
    }
}

@Composable
@ReadOnlyComposable
fun formatSpeed(
    speed: Float?,
    unit: SpeedUnit = SpeedUnit.METERS_PER_SECOND
): String {
    if (speed == null || speed.isNaN()) return stringResource(R.string.not_available_short)

    val speedValue = speed.roundToInt()
    return when (unit) {
        SpeedUnit.METERS_PER_SECOND -> stringResource(R.string.speed_format_ms, speedValue)
        SpeedUnit.KILOMETERS_PER_HOUR -> stringResource(R.string.speed_format_kmh, speedValue)
        SpeedUnit.MILES_PER_HOUR -> stringResource(R.string.speed_format_mph, speedValue)
    }
}

@Composable
@ReadOnlyComposable
fun formatPressure(
    pressure: Float?,
    unit: PressureUnit = PressureUnit.HECTOPASCALS
): String {
    if (pressure == null || pressure.isNaN()) return stringResource(R.string.not_available_short)

    val pressureValue = pressure.roundToInt()
    return when (unit) {
        PressureUnit.HECTOPASCALS -> stringResource(R.string.pressure_format_hpa, pressureValue)
        PressureUnit.MILLIMETERS_OF_MERCURY -> stringResource(R.string.pressure_format_mmhg, pressureValue)
        PressureUnit.INCHES_OF_MERCURY -> {
            val pressureInHg = "%.2f".format(Locale.US, pressure)
            stringResource(R.string.pressure_format_inhg, pressureInHg)
        }
    }
}

@Composable
fun LocalDateTime.formatAsTime(): String {
    val current = LocalTimeFormatter.current
    val formatter = remember { current }
    return try {
        this.toJavaLocalDateTime().format(formatter)
    } catch (e: Exception) {
        stringResource(R.string.not_available_short)
    }
}

@Composable
fun LocalDate.formatAsDayMonth(): String {
    val current = LocalDayMonthFormatter.current
    val formatter = remember { current }
    return try {
        this.toJavaLocalDate().format(formatter)
    } catch (e: Exception) {
        stringResource(R.string.not_available_short)
    }
}

@Composable
fun LocalDate.formatAsDayOfWeek(): String {
    val current = LocalDayOfWeekFormatter.current
    val formatter = remember { current }
    return try {
        this.toJavaLocalDate().format(formatter)
    } catch (e: Exception) {
        stringResource(R.string.not_available_short)
    }
}

fun LocalDate.format(formatter: DateTimeFormatter): String {
    return try {
        this.toJavaLocalDate().format(formatter)
    } catch (e: Exception) {
        "N/A"
    }
}

@Composable
@ReadOnlyComposable
fun getWeatherDescription(condition: WeatherCondition?): String {
    val resourceId = when (condition) {
        WeatherCondition.CLEAR_SKY_DAY -> R.string.weather_clear_sky
        WeatherCondition.CLEAR_SKY_NIGHT -> R.string.weather_clear_sky_night
        WeatherCondition.FEW_CLOUDS_DAY -> R.string.weather_few_clouds_day
        WeatherCondition.FEW_CLOUDS_NIGHT -> R.string.weather_few_clouds_night
        WeatherCondition.SCATTERED_CLOUDS -> R.string.weather_scattered_clouds
        WeatherCondition.BROKEN_CLOUDS -> R.string.weather_broken_clouds
        WeatherCondition.OVERCAST_CLOUDS -> R.string.weather_overcast_clouds
        WeatherCondition.MIST -> R.string.weather_mist
        WeatherCondition.SMOKE -> R.string.weather_smoke
        WeatherCondition.HAZE -> R.string.weather_haze
        WeatherCondition.DUST_WHIRLS, WeatherCondition.DUST -> R.string.weather_dust
        WeatherCondition.SAND -> R.string.weather_sand
        WeatherCondition.VOLCANIC_ASH -> R.string.weather_volcanic_ash
        WeatherCondition.SQUALLS -> R.string.weather_squalls
        WeatherCondition.TORNADO -> R.string.weather_tornado
        WeatherCondition.FOG -> R.string.weather_fog
        WeatherCondition.LIGHT_RAIN -> R.string.weather_light_rain
        WeatherCondition.MODERATE_RAIN -> R.string.weather_moderate_rain
        WeatherCondition.HEAVY_RAIN -> R.string.weather_heavy_rain
        WeatherCondition.VERY_HEAVY_RAIN, WeatherCondition.EXTREME_RAIN -> R.string.weather_extreme_rain
        WeatherCondition.FREEZING_RAIN -> R.string.weather_freezing_rain
        WeatherCondition.LIGHT_SHOWER_RAIN -> R.string.weather_light_shower_rain
        WeatherCondition.SHOWER_RAIN -> R.string.weather_shower_rain
        WeatherCondition.HEAVY_SHOWER_RAIN, WeatherCondition.RAGGED_SHOWER_RAIN -> R.string.weather_heavy_shower_rain
        WeatherCondition.THUNDERSTORM_LIGHT_RAIN,
        WeatherCondition.THUNDERSTORM_RAIN,
        WeatherCondition.THUNDERSTORM_HEAVY_RAIN,
        WeatherCondition.LIGHT_THUNDERSTORM,
        WeatherCondition.THUNDERSTORM,
        WeatherCondition.HEAVY_THUNDERSTORM,
        WeatherCondition.RAGGED_THUNDERSTORM,
        WeatherCondition.THUNDERSTORM_LIGHT_DRIZZLE,
        WeatherCondition.THUNDERSTORM_DRIZZLE,
        WeatherCondition.THUNDERSTORM_HEAVY_DRIZZLE -> R.string.weather_thunderstorm
        WeatherCondition.LIGHT_SNOW -> R.string.weather_light_snow
        WeatherCondition.SNOW -> R.string.weather_snow
        WeatherCondition.HEAVY_SNOW -> R.string.weather_heavy_snow
        WeatherCondition.SLEET, WeatherCondition.LIGHT_SHOWER_SLEET, WeatherCondition.SHOWER_SLEET,
        WeatherCondition.LIGHT_RAIN_SNOW, WeatherCondition.RAIN_SNOW -> R.string.weather_sleet
        WeatherCondition.LIGHT_SHOWER_SNOW, WeatherCondition.SHOWER_SNOW, WeatherCondition.HEAVY_SHOWER_SNOW -> R.string.weather_shower_snow
        WeatherCondition.LIGHT_DRIZZLE -> R.string.weather_light_drizzle
        WeatherCondition.DRIZZLE -> R.string.weather_drizzle
        WeatherCondition.HEAVY_DRIZZLE -> R.string.weather_heavy_drizzle
        WeatherCondition.LIGHT_DRIZZLE_RAIN, WeatherCondition.DRIZZLE_RAIN, WeatherCondition.HEAVY_DRIZZLE_RAIN -> R.string.weather_drizzle_rain
        WeatherCondition.SHOWER_DRIZZLE -> R.string.weather_shower_drizzle
        WeatherCondition.UNKNOWN, null -> R.string.weather_unknown
    }
    return stringResource(id = resourceId)
}