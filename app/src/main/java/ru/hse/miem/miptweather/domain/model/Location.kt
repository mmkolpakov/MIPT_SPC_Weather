package ru.hse.miem.miptweather.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Location(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val countryCode: String = "",
    val timezone: String = ""
) {
    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun getDisplayName(): String = name.ifBlank {
        "Lat: ${String.format(Locale.US, "%.3f", latitude)}, Lon: ${String.format(Locale.US, "%.3f", longitude)}"
    }
}