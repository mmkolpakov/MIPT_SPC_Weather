package ru.hse.miem.miptweather.presentation.location

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import ru.hse.miem.miptweather.domain.usecase.GetCurrentLocationUseCase
import ru.hse.miem.miptweather.domain.util.Result as DomainResult
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed class LocationViewModelEvent {
    data object LocationSaved : LocationViewModelEvent()
}

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    private companion object { const val TAG = "LocationViewModel" }

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _editingLocation = MutableStateFlow<Location?>(null)

    private var isFetchingGps = false

    private val _eventChannel = Channel<LocationViewModelEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    val savedLocations: StateFlow<List<Location>> = locationRepository
        .getSavedLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _editingLocation.collect { editingLoc ->
                _uiState.update { it.copy(isEditing = editingLoc != null) }
            }
        }
    }

    fun useCurrentLocation() = viewModelScope.launch {
        if (isFetchingGps) {
            Log.d(TAG, "useCurrentLocation called while already fetching GPS.")
            return@launch
        }

        isFetchingGps = true
        _uiState.update { it.copy(isSaving = true, locationError = null, saveErrorResId = null) }
        Log.d(TAG, "Starting GPS location fetch...")

        try {
            getCurrentLocationUseCase().collect { result ->
                handleLocationResult(result)
                if (result !is DomainResult.Loading) {
                    isFetchingGps = false
                    Log.d(TAG, "GPS fetch finished with result: ${result::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.d(TAG, "GPS fetch cancelled.")
            } else {
                Log.e(TAG, "Error during GPS location fetch flow subscription", e)
                _uiState.update {
                    it.copy(
                        locationError = e.localizedMessage ?: "Unknown location fetch error",
                        isSaving = false
                    )
                }
            }
        } finally {
            isFetchingGps = false
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false) }
                Log.w(TAG, "isSaving was still true in finally block (GPS fetch), resetting.")
            }
            Log.d(TAG, "GPS fetch flow finally block executed.")
        }
    }

    private fun handleLocationResult(result: DomainResult<Location>) {
        when (result) {
            is DomainResult.Success -> {
                val loc = result.data
                Log.d(TAG, "GPS Success: Lat=${loc.latitude}, Lon=${loc.longitude}, Name='${loc.name}'")
                _uiState.update {
                    it.copy(
                        latitude = String.format(Locale.US, "%.6f", loc.latitude),
                        longitude = String.format(Locale.US, "%.6f", loc.longitude),
                        locationName = loc.name.takeIf(String::isNotBlank) ?: it.locationName,
                        latitudeErrorResId = null,
                        longitudeErrorResId = null,
                        locationError = null,
                        isSaving = false
                    )
                }
            }
            is DomainResult.Error -> {
                Log.w(TAG, "GPS Error: ${result.exception.message}")
                _uiState.update {
                    it.copy(
                        locationError = result.exception.localizedMessage ?: "Failed to get location",
                        isSaving = false
                    )
                }
            }
            is DomainResult.Loading -> {
                Log.d(TAG, "GPS Loading...")
            }
        }
    }

    fun updateLatitude(input: String) = updateField(
        field = Field.LATITUDE,
        value = input
    )

    fun updateLongitude(input: String) = updateField(
        field = Field.LONGITUDE,
        value = input
    )

    fun updateLocationName(name: String) = _uiState.update { it.copy(locationName = name.take(100)) }

    fun startEditing(location: Location) {
        _editingLocation.value = location
        _uiState.update {
            it.copy(
                latitude = String.format(Locale.US, "%.6f", location.latitude),
                longitude = String.format(Locale.US, "%.6f", location.longitude),
                locationName = location.name,
                latitudeErrorResId = null,
                longitudeErrorResId = null,
                saveErrorResId = null,
                locationError = null,
                isSaving = false
            )
        }
    }

    fun saveLocation() = viewModelScope.launch {
        if (!validateCoordinates()) return@launch

        _uiState.update { it.copy(isSaving = true, saveErrorResId = null, locationError = null) }

        val currentState = _uiState.value
        val editing = _editingLocation.value
        val latitude = currentState.latitude.replace(',', '.').toDouble()
        val longitude = currentState.longitude.replace(',', '.').toDouble()
        val name = currentState.locationName.trim()

        val locationToSave = (editing?.copy(
            latitude = latitude,
            longitude = longitude,
            name = name.ifBlank { Location(0, latitude, longitude).getDisplayName() }
        ) ?: Location(
            latitude = latitude,
            longitude = longitude,
            name = name
        )).let { it.copy(name = it.getDisplayName()) }

        var saveSuccess = false
        try {
            val existingLocationId = if (editing != null) {
                locationRepository.findSavedLocationByCoords(editing.latitude, editing.longitude)?.id
            } else {
                null
            }

            if (existingLocationId != null) {
                Log.d(TAG, "Updating existing location ID: $existingLocationId")
                locationRepository.updateLocation(locationToSave.copy(id = existingLocationId))
            } else {
                Log.d(TAG, "Saving new location.")
                locationRepository.saveLocation(locationToSave)
            }
            saveSuccess = true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error saving location", e)
            _uiState.update { it.copy(saveErrorResId = R.string.error_saving_location, isSaving = false) }
        } finally {
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false) }
            }
            if (saveSuccess) {
                resetDraft()
                _eventChannel.send(LocationViewModelEvent.LocationSaved)
                Log.d(TAG, "Save successful, event sent.")
            }
        }
    }


    fun deleteLocation(location: Location) = viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true, saveErrorResId = null) }
        try {
            val entityToDelete = locationRepository.findSavedLocationByCoords(location.latitude, location.longitude)
            if (entityToDelete != null) {
                Log.d(TAG, "Deleting location ID: ${entityToDelete.id}")
                locationRepository.deleteLocation(entityToDelete)
            } else {
                Log.w(TAG, "Location to delete not found in DB: $location")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error deleting location", e)
            _uiState.update {
                it.copy(saveErrorResId = R.string.error_deleting_location)
            }
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun resetDraft() {
        _editingLocation.value = null
        _uiState.update {
            LocationUiState(isEditing = false, isSaving = false)
        }
        Log.d(TAG, "Draft reset.")
    }

    fun clearLocationError() = _uiState.update { it.copy(locationError = null) }
    fun clearSaveError() = _uiState.update { it.copy(saveErrorResId = null) }

    private fun updateField(field: Field, value: String) {
        val normalizedValue = value.replace(',', '.')
        _uiState.update {
            when (field) {
                Field.LATITUDE -> it.copy(
                    latitude = value,
                    latitudeErrorResId = validateCoordinate(normalizedValue, -90.0, 90.0)
                )
                Field.LONGITUDE -> it.copy(
                    longitude = value,
                    longitudeErrorResId = validateCoordinate(normalizedValue, -180.0, 180.0)
                )
            }
        }
    }

    private fun validateCoordinates(): Boolean {
        val state = _uiState.value
        val latErr = validateCoordinate(state.latitude.replace(',', '.'), -90.0, 90.0)
        val lonErr = validateCoordinate(state.longitude.replace(',', '.'), -180.0, 180.0)
        val isValid = latErr == null && lonErr == null
        if (!isValid) {
            _uiState.update { it.copy(latitudeErrorResId = latErr, longitudeErrorResId = lonErr) }
        }
        return isValid
    }

    @StringRes
    private fun validateCoordinate(normalizedInput: String, min: Double, max: Double): Int? {
        if (normalizedInput.isBlank()) return R.string.error_field_required
        val value = normalizedInput.toDoubleOrNull()
            ?: return R.string.error_invalid_number
        return if (value !in min..max) R.string.error_out_of_range else null
    }

    private enum class Field { LATITUDE, LONGITUDE }
}