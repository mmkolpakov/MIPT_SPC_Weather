package ru.hse.miem.miptweather.domain.model

import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit

data class UnitPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.METERS_PER_SECOND,
    val pressureUnit: PressureUnit = PressureUnit.HECTOPASCALS
)