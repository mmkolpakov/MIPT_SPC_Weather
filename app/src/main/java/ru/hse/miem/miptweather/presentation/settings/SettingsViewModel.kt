package ru.hse.miem.miptweather.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.hse.miem.miptweather.domain.model.UnitPreferences
import ru.hse.miem.miptweather.domain.repository.SettingsRepository
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import ru.hse.miem.miptweather.domain.usecase.GetAvailableProvidersUseCase
import ru.hse.miem.miptweather.domain.usecase.SetPreferredProviderUseCase
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository,
    private val getAvailableProvidersUseCase: GetAvailableProvidersUseCase,
    private val setPreferredProviderUseCase: SetPreferredProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                getAvailableProvidersUseCase(),
                weatherRepository.getPreferredProvider(),
                settingsRepository.getUnitPreferences()
            ) { providers, preferred, units ->
                SettingsUiState(
                    availableProviders = providers,
                    preferredProvider = preferred,
                    temperatureUnit = units.temperatureUnit,
                    speedUnit = units.speedUnit,
                    pressureUnit = units.pressureUnit,
                    isLoading = false,
                    errorLoading = false
                )
            }.catch { e ->
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorLoading = true
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setPreferredProvider(providerId: String) {
        val currentState = _uiState.value
        if (currentState.preferredProvider != providerId) {
            _uiState.update { it.copy(preferredProvider = providerId) }
            viewModelScope.launch {
                try {
                    setPreferredProviderUseCase(providerId)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        if (_uiState.value.temperatureUnit != unit) {
            _uiState.update { it.copy(temperatureUnit = unit) }
            saveUnitPreferences()
        }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        if (_uiState.value.speedUnit != unit) {
            _uiState.update { it.copy(speedUnit = unit) }
            saveUnitPreferences()
        }
    }

    fun setPressureUnit(unit: PressureUnit) {
        if (_uiState.value.pressureUnit != unit) {
            _uiState.update { it.copy(pressureUnit = unit) }
            saveUnitPreferences()
        }
    }

    private fun saveUnitPreferences() {
        val currentUnits = _uiState.value
        viewModelScope.launch {
            try {
                settingsRepository.saveUnitPreferences(
                    UnitPreferences(
                        temperatureUnit = currentUnits.temperatureUnit,
                        speedUnit = currentUnits.speedUnit,
                        pressureUnit = currentUnits.pressureUnit
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun retryLoadSettings() {
        _uiState.update { it.copy(isLoading = true, errorLoading = false) }
        loadSettings()
    }
}