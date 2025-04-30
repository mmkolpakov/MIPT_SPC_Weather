package ru.hse.miem.miptweather.presentation.common

import androidx.annotation.DrawableRes
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.WeatherCondition

object WeatherIcons {
    @DrawableRes
    fun getIconResource(condition: WeatherCondition?): Int {
        return when (condition) {
            WeatherCondition.CLEAR_SKY_DAY -> R.drawable.ic_clear_sky
            WeatherCondition.CLEAR_SKY_NIGHT -> R.drawable.ic_clear_sky_night
            WeatherCondition.FEW_CLOUDS_DAY -> R.drawable.ic_few_clouds_day
            WeatherCondition.FEW_CLOUDS_NIGHT -> R.drawable.ic_few_clouds_night
            WeatherCondition.SCATTERED_CLOUDS -> R.drawable.ic_scattered_clouds
            WeatherCondition.BROKEN_CLOUDS -> R.drawable.ic_broken_clouds
            WeatherCondition.OVERCAST_CLOUDS -> R.drawable.ic_overcast_clouds
            WeatherCondition.MIST -> R.drawable.ic_mist
            WeatherCondition.FOG -> R.drawable.ic_mist
            WeatherCondition.SMOKE -> R.drawable.ic_mist
            WeatherCondition.HAZE -> R.drawable.ic_mist
            WeatherCondition.DUST_WHIRLS -> R.drawable.ic_wind
            WeatherCondition.DUST -> R.drawable.ic_wind
            WeatherCondition.SAND -> R.drawable.ic_wind
            WeatherCondition.VOLCANIC_ASH -> R.drawable.ic_wind
            WeatherCondition.SQUALLS -> R.drawable.ic_wind
            WeatherCondition.TORNADO -> R.drawable.ic_tornado
            WeatherCondition.LIGHT_RAIN -> R.drawable.ic_rain
            WeatherCondition.MODERATE_RAIN -> R.drawable.ic_rain
            WeatherCondition.HEAVY_RAIN -> R.drawable.ic_rain
            WeatherCondition.VERY_HEAVY_RAIN -> R.drawable.ic_rain
            WeatherCondition.EXTREME_RAIN -> R.drawable.ic_rain
            WeatherCondition.SHOWER_RAIN -> R.drawable.ic_rain
            WeatherCondition.LIGHT_SHOWER_RAIN -> R.drawable.ic_rain
            WeatherCondition.HEAVY_SHOWER_RAIN -> R.drawable.ic_rain
            WeatherCondition.RAGGED_SHOWER_RAIN -> R.drawable.ic_rain
            WeatherCondition.FREEZING_RAIN -> R.drawable.ic_sleet
            WeatherCondition.LIGHT_DRIZZLE -> R.drawable.ic_drizzle
            WeatherCondition.DRIZZLE -> R.drawable.ic_drizzle
            WeatherCondition.HEAVY_DRIZZLE -> R.drawable.ic_drizzle
            WeatherCondition.LIGHT_DRIZZLE_RAIN -> R.drawable.ic_drizzle
            WeatherCondition.DRIZZLE_RAIN -> R.drawable.ic_drizzle
            WeatherCondition.HEAVY_DRIZZLE_RAIN -> R.drawable.ic_drizzle
            WeatherCondition.SHOWER_DRIZZLE -> R.drawable.ic_drizzle
            WeatherCondition.THUNDERSTORM_LIGHT_RAIN -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM_RAIN -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM_HEAVY_RAIN -> R.drawable.ic_thunderstorm
            WeatherCondition.LIGHT_THUNDERSTORM -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM -> R.drawable.ic_thunderstorm
            WeatherCondition.HEAVY_THUNDERSTORM -> R.drawable.ic_thunderstorm
            WeatherCondition.RAGGED_THUNDERSTORM -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM_LIGHT_DRIZZLE -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM_DRIZZLE -> R.drawable.ic_thunderstorm
            WeatherCondition.THUNDERSTORM_HEAVY_DRIZZLE -> R.drawable.ic_thunderstorm
            WeatherCondition.LIGHT_SNOW -> R.drawable.ic_snow
            WeatherCondition.SNOW -> R.drawable.ic_snow
            WeatherCondition.HEAVY_SNOW -> R.drawable.ic_snow
            WeatherCondition.LIGHT_SHOWER_SNOW -> R.drawable.ic_snow
            WeatherCondition.SHOWER_SNOW -> R.drawable.ic_snow
            WeatherCondition.HEAVY_SHOWER_SNOW -> R.drawable.ic_snow
            WeatherCondition.SLEET -> R.drawable.ic_sleet
            WeatherCondition.LIGHT_SHOWER_SLEET -> R.drawable.ic_sleet
            WeatherCondition.SHOWER_SLEET -> R.drawable.ic_sleet
            WeatherCondition.LIGHT_RAIN_SNOW -> R.drawable.ic_sleet
            WeatherCondition.RAIN_SNOW -> R.drawable.ic_sleet
            WeatherCondition.UNKNOWN, null -> R.drawable.ic_unknown_weather
        }
    }

    @DrawableRes
    fun getEmptyIcon(): Int {
        return R.drawable.ic_cloud_off
    }

    @DrawableRes
    fun getErrorIcon(): Int {
        return R.drawable.ic_error_outline
    }
}