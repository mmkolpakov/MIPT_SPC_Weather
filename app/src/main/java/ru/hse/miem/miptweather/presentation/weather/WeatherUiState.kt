package ru.hse.miem.miptweather.presentation.weather

import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherAnalysis
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit


sealed class ErrorType {
    data class Network(val message: String, val isConnectivityIssue: Boolean) : ErrorType()
    data class LocationNotFound(val message: String) : ErrorType()
    data class DataProcessing(val message: String) : ErrorType()
    data class Unknown(val message: String) : ErrorType()
}

data class WeatherUiState(
    val weatherData: WeatherData? = null,
    val analysis: WeatherAnalysis? = null,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val errorState: ErrorType? = null,
    val location: Location? = null,
    val dateRange: DateRange? = null,
    val preferredProvider: String? = null,
    val requiresLocationSelection: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.METERS_PER_SECOND,
    val pressureUnit: PressureUnit = PressureUnit.HECTOPASCALS
)