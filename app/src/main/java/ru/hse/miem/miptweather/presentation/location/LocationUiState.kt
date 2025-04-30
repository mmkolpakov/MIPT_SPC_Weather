package ru.hse.miem.miptweather.presentation.location

import androidx.annotation.StringRes

data class LocationUiState(
    val latitude: String = "",
    val longitude: String = "",
    val locationName: String = "",
    @StringRes val latitudeErrorResId: Int? = null,
    @StringRes val longitudeErrorResId: Int? = null,
    @StringRes val saveErrorResId: Int? = null,
    val isSaving: Boolean = false,
    val isFetchingGps: Boolean = false,
    val isEditing: Boolean = false,
    val locationError: String? = null
)