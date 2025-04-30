package ru.hse.miem.miptweather.domain.usecase

import ru.hse.miem.miptweather.domain.model.*
import ru.hse.miem.miptweather.domain.model.WeatherCondition // Added import
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class AnalyzeWeatherDataUseCase @Inject constructor() {

    private companion object {
        const val MIN_PRECIPITATION_FOR_RAIN_DAY = 1.0f
        const val MIN_WIND_SPEED_FOR_WINDY_DAY = 5.5f
    }

    operator fun invoke(weatherData: WeatherData): WeatherAnalysis? {
        val hourly = weatherData.hourly.filterNot { it.temperature.isNaN() }
        val daily = weatherData.daily.filterNot { it.minTemperature.isNaN() || it.maxTemperature.isNaN() }

        if (hourly.isEmpty() && daily.isEmpty()) return null

        val maxTempHourly = hourly.maxByOrNull { it.temperature }
        val minTempHourly = hourly.minByOrNull { it.temperature }
        val maxTempDaily = daily.maxByOrNull { it.maxTemperature }
        val minTempDaily = daily.minByOrNull { it.minTemperature }

        val overallMaxTemp = maxTempHourly?.temperature ?: maxTempDaily?.maxTemperature ?: Float.NaN
        val overallMinTemp = minTempHourly?.temperature ?: minTempDaily?.minTemperature ?: Float.NaN

        if (overallMaxTemp.isNaN() || overallMinTemp.isNaN()) return null

        val maxTempDate = maxTempHourly?.dateTime?.date ?: maxTempDaily?.date ?: weatherData.dateRange.endDate
        val minTempDate = minTempHourly?.dateTime?.date ?: minTempDaily?.date ?: weatherData.dateRange.startDate
        val maxTempTime = maxTempHourly?.dateTime
        val minTempTime = minTempHourly?.dateTime

        val avgTemp: Float
        val stdDev: Float?

        if (hourly.isNotEmpty()) {
            val temps = hourly.map { it.temperature }
            avgTemp = temps.average().toFloat()
            val variance = temps.map { (it - avgTemp).pow(2) }.average()
            stdDev = sqrt(variance).toFloat()
        } else {
            val dailyAvgTemps = daily.mapNotNull { it.avgTemperature ?: ((it.minTemperature + it.maxTemperature) / 2f).takeUnless { temp -> temp.isNaN() } }
            avgTemp = if (dailyAvgTemps.isNotEmpty()) dailyAvgTemps.average().toFloat() else Float.NaN
            stdDev = null
        }

        val rainDays = daily.count { it.precipitationSum?.let { p -> p >= MIN_PRECIPITATION_FOR_RAIN_DAY } ?: false }
        val totalPrecipitation = daily.sumOf { it.precipitationSum?.toDouble() ?: 0.0 }.toFloat()
        val windyDays = daily.count { it.maxWindSpeed?.let { w -> w >= MIN_WIND_SPEED_FOR_WINDY_DAY } ?: false }
        val overcastDays = daily.count { it.weatherCondition == WeatherCondition.OVERCAST_CLOUDS || it.weatherCondition == WeatherCondition.BROKEN_CLOUDS }


        return WeatherAnalysis(
            minTemperature = TemperatureExtreme(overallMinTemp, minTempDate, minTempTime),
            maxTemperature = TemperatureExtreme(overallMaxTemp, maxTempDate, maxTempTime),
            avgTemperature = avgTemp,
            temperatureAmplitude = if (avgTemp.isNaN() || overallMinTemp.isNaN() || overallMaxTemp.isNaN()) Float.NaN else overallMaxTemp - overallMinTemp,
            temperatureStdDev = stdDev,
            rainDays = rainDays,
            totalPrecipitation = totalPrecipitation,
            windyDays = windyDays,
            overcastDays = overcastDays
        )
    }
}