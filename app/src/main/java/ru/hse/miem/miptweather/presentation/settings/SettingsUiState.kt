package ru.hse.miem.miptweather.presentation.settings

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class SpeedUnit { METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR }
enum class PressureUnit { HECTOPASCALS, MILLIMETERS_OF_MERCURY, INCHES_OF_MERCURY }

data class SettingsUiState(
    val availableProviders: List<String> = emptyList(),
    val preferredProvider: String = "",
    val isLoading: Boolean = true,
    val errorLoading: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.METERS_PER_SECOND,
    val pressureUnit: PressureUnit = PressureUnit.HECTOPASCALS
)