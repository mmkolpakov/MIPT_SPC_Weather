package ru.hse.miem.miptweather.presentation.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.io.IOException
import ru.hse.miem.miptweather.data.api.ApiException
import ru.hse.miem.miptweather.data.api.NoConnectivityException
import ru.hse.miem.miptweather.domain.model.*
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import ru.hse.miem.miptweather.domain.repository.SettingsRepository
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import ru.hse.miem.miptweather.domain.usecase.*
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import ru.hse.miem.miptweather.domain.util.Result as DomainResult
import ru.hse.miem.miptweather.domain.model.UnitPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getWeatherDataUseCase: GetWeatherDataUseCase,
    private val analyzeWeatherDataUseCase: AnalyzeWeatherDataUseCase,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private companion object { const val TAG = "WeatherViewModel" }

    private val _uiState = MutableStateFlow(WeatherUiState(isInitialLoading = true))
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    private val _dateRange = MutableStateFlow(createDefaultDateRange())
    private val _triggerRefresh = MutableStateFlow(Unit)

    init {
        viewModelScope.launch {
            val lastLocation = locationRepository.getSavedLocations().firstOrNull()?.firstOrNull()
            if (lastLocation != null) {
                Log.d(TAG, "Loaded initial location: ${lastLocation.getDisplayName()}")
                _location.value = lastLocation
            } else {
                Log.d(TAG, "No saved locations found initially.")
                updateRequiresLocationSelection()
                if (_location.value == null) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        settingsRepository.getUnitPreferences()
            .onEach { units: UnitPreferences ->
                Log.d(TAG, "Unit preferences updated: $units")
                _uiState.update { currentState ->
                    val convertedData = currentState.weatherData?.let { wd ->
                        convertWeatherDataUnits(wd, units)
                    }
                    val convertedAnalysis = convertedData?.let { analyzeWeatherDataUseCase(it) }
                    currentState.copy(
                        temperatureUnit = units.temperatureUnit,
                        speedUnit = units.speedUnit,
                        pressureUnit = units.pressureUnit,
                        weatherData = convertedData ?: currentState.weatherData,
                        analysis = convertedAnalysis ?: currentState.analysis
                    )
                }
            }
            .catch { e -> Log.e(TAG, "Error collecting unit preferences", e) }
            .launchIn(viewModelScope)

        weatherRepository.getPreferredProvider()
            .onEach { providerId ->
                Log.d(TAG, "Preferred provider updated: $providerId")
                _uiState.update { it.copy(preferredProvider = providerId) }
            }
            .catch { e -> Log.e(TAG, "Error collecting preferred provider", e) }
            .launchIn(viewModelScope)

        locationRepository.getSavedLocations()
            .map { it.size }
            .distinctUntilChanged()
            .onEach { updateRequiresLocationSelection() }
            .catch { e -> Log.e(TAG, "Error collecting saved locations count", e) }
            .launchIn(viewModelScope)


        combine(_location, _dateRange, _triggerRefresh) { location, dateRange, _ ->
            Pair(location, dateRange)
        }
            .filter { (location, _) -> location != null }
            .flatMapLatest { (location, dateRange) ->
                Log.d(TAG, "Triggering weather data fetch for ${location?.getDisplayName()}, range $dateRange")
                getWeatherDataUseCase(location!!, dateRange, _uiState.value.isLoading)
                    .onStart {
                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                errorState = null,
                                isInitialLoading = it.weatherData == null
                            )
                        }
                        Log.d(TAG, "Weather data flow started (isLoading=true)")
                    }
                    .map { result -> Pair(result, location) }
            }
            .onEach { (result, locationUsed) -> processWeatherResult(result, locationUsed) }
            .catch { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "Error in weather data flow collection", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorState = determineErrorType(e)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setLocation(location: Location) {
        if (_location.value != location) {
            Log.i(TAG, "Setting new location: ${location.getDisplayName()}")
            _uiState.update { it.copy(weatherData = null, analysis = null, isInitialLoading = true) }
            _location.value = location
            updateRequiresLocationSelection()
        } else {
            Log.d(TAG, "setLocation called with the same location, ignoring.")
        }
    }

    fun setDateRange(dateRange: DateRange) {
        if (dateRange.isValid() && dateRange.days <= DateRange.MAX_RANGE_DAYS) {
            if (_dateRange.value != dateRange) {
                Log.i(TAG, "Setting new date range: $dateRange")
                _dateRange.value = dateRange
            } else {
                Log.d(TAG, "setDateRange called with the same date range, ignoring.")
            }
        } else {
            Log.w(TAG, "Attempted to set invalid date range: $dateRange")
        }
    }

    fun refreshWeatherData() {
        if (_location.value != null && !_uiState.value.isLoading) {
            Log.i(TAG, "Manual refresh triggered.")
            _triggerRefresh.value = Unit
        } else {
            Log.w(TAG, "Refresh requested but location is null or already loading.")
        }
    }

    fun clearError() {
        Log.d(TAG, "Dismissing error.")
        _uiState.update { it.copy(errorState = null) }
    }

    private fun processWeatherResult(result: DomainResult<WeatherData>, locationUsed: Location) {
        Log.d(TAG, "Processing weather result: ${result::class.simpleName}")
        when (result) {
            is DomainResult.Success -> {
                val currentState = _uiState.value
                val currentUnits = UnitPreferences(
                    temperatureUnit = currentState.temperatureUnit,
                    speedUnit = currentState.speedUnit,
                    pressureUnit = currentState.pressureUnit
                )
                val convertedData = convertWeatherDataUnits(result.data, currentUnits)
                val analysisResult = analyzeWeatherDataUseCase(convertedData)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isInitialLoading = false,
                        weatherData = convertedData,
                        analysis = analysisResult,
                        errorState = null,
                        location = locationUsed.takeIf { loc -> loc.name.isNotBlank() || loc.timezone.isNotBlank() } ?: it.location
                    )
                }
                viewModelScope.launch {
                    updateLocationDetailsIfNeeded(locationUsed)
                }
            }
            is DomainResult.Error -> {
                Log.e(TAG, "Weather data fetch error: ${result.exception.message}", result.exception)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isInitialLoading = false,
                        errorState = determineErrorType(result.exception)
                    )
                }
            }
            is DomainResult.Loading -> {

            }
        }
    }

    private suspend fun updateLocationDetailsIfNeeded(locationFromApi: Location) {
        val currentLocation = _location.value ?: return
        val currentDbDetails = locationRepository.getLocationById(currentLocation.id) ?: currentLocation

        val needsUpdate = (locationFromApi.name.isNotBlank() && locationFromApi.name != currentDbDetails.name) ||
                (locationFromApi.countryCode.isNotBlank() && locationFromApi.countryCode != currentDbDetails.countryCode) ||
                (locationFromApi.timezone.isNotBlank() && locationFromApi.timezone != currentDbDetails.timezone)

        if (needsUpdate) {
            val updatedLocation = currentDbDetails.copy(
                name = locationFromApi.name.takeIf { it.isNotBlank() } ?: currentDbDetails.name,
                countryCode = locationFromApi.countryCode.takeIf { it.isNotBlank() } ?: currentDbDetails.countryCode,
                timezone = locationFromApi.timezone.takeIf { it.isNotBlank() } ?: currentDbDetails.timezone
            )
            Log.d(TAG, "Updating location details in DB for ID ${updatedLocation.id}")
            locationRepository.updateLocation(updatedLocation)
            if (_location.value?.id == updatedLocation.id) {
                _location.value = updatedLocation
            }
        }
    }

    private fun updateRequiresLocationSelection() {
        viewModelScope.launch {
            val loc = _location.value
            val count = locationRepository.getSavedLocations().firstOrNull()?.size ?: 0
            val requiresSelection = loc == null && count == 0
            Log.d(TAG, "Updating requiresLocationSelection: $requiresSelection (loc is null: ${loc == null}, count: $count)")
            _uiState.update { it.copy(requiresLocationSelection = requiresSelection) }
        }
    }

    private fun determineErrorType(exception: Throwable): ErrorType {
        return when (exception) {
            is NoConnectivityException -> ErrorType.Network(exception.message ?: "Нет подключения", true)
            is ApiException -> ErrorType.Network(exception.message ?: "Ошибка сети", false)
            is IOException -> ErrorType.Network("Ошибка ввода-вывода", true)
            is IllegalArgumentException -> ErrorType.DataProcessing(exception.message ?: "Неверные данные")
            is IllegalStateException -> ErrorType.LocationNotFound(exception.message ?: "Локация не найдена")
            else -> ErrorType.Unknown(exception.message ?: "Неизвестная ошибка")
        }
    }

    private fun createDefaultDateRange(): DateRange {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val endDate = today.plus(6, DateTimeUnit.DAY)
        return DateRange(today, endDate)
    }

    private fun convertWeatherDataUnits(data: WeatherData, units: UnitPreferences): WeatherData {
        if (units.temperatureUnit == TemperatureUnit.CELSIUS &&
            units.speedUnit == SpeedUnit.METERS_PER_SECOND &&
            units.pressureUnit == PressureUnit.HECTOPASCALS) {
            return data
        }
        Log.d(TAG, "Converting weather data units to: $units")

        val originalData = _uiState.value.weatherData?.takeIf { it.location == data.location && it.dateRange == data.dateRange } ?: data

        return originalData.copy(
            hourly = originalData.hourly.map { hour ->
                hour.copy(
                    temperature = convertTemperature(hour.temperature, units.temperatureUnit),
                    feelsLike = hour.feelsLike?.let { convertTemperature(it, units.temperatureUnit) },
                    windSpeed = hour.windSpeed?.let { convertSpeed(it, units.speedUnit) },
                    pressure = hour.pressure?.let { convertPressure(it, units.pressureUnit) }
                )
            },
            daily = originalData.daily.map { day ->
                day.copy(
                    minTemperature = convertTemperature(day.minTemperature, units.temperatureUnit),
                    maxTemperature = convertTemperature(day.maxTemperature, units.temperatureUnit),
                    avgTemperature = day.avgTemperature?.let { convertTemperature(it, units.temperatureUnit) },
                    minFeelsLike = day.minFeelsLike?.let { convertTemperature(it, units.temperatureUnit) },
                    maxFeelsLike = day.maxFeelsLike?.let { convertTemperature(it, units.temperatureUnit) },
                    maxWindSpeed = day.maxWindSpeed?.let { convertSpeed(it, units.speedUnit) }
                )
            }
        )
    }

    private fun convertTemperature(valueCelsius: Float, targetUnit: TemperatureUnit): Float {
        if (valueCelsius.isNaN()) return Float.NaN
        return when (targetUnit) {
            TemperatureUnit.FAHRENHEIT -> (valueCelsius * 9f / 5f) + 32f
            TemperatureUnit.CELSIUS -> valueCelsius
        }
    }

    private fun convertSpeed(valueMs: Float, targetUnit: SpeedUnit): Float {
        if (valueMs.isNaN()) return Float.NaN
        return when (targetUnit) {
            SpeedUnit.KILOMETERS_PER_HOUR -> valueMs * 3.6f
            SpeedUnit.MILES_PER_HOUR -> valueMs * 2.23694f
            SpeedUnit.METERS_PER_SECOND -> valueMs
        }
    }

    private fun convertPressure(valueHpa: Float, targetUnit: PressureUnit): Float {
        if (valueHpa.isNaN()) return Float.NaN
        return when (targetUnit) {
            PressureUnit.MILLIMETERS_OF_MERCURY -> valueHpa * 0.750062f
            PressureUnit.INCHES_OF_MERCURY -> valueHpa * 0.02953f
            PressureUnit.HECTOPASCALS -> valueHpa
        }
    }
}