package ru.hse.miem.miptweather.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.hse.miem.miptweather.data.api.ApiException
import ru.hse.miem.miptweather.data.api.WeatherProvider
import ru.hse.miem.miptweather.data.api.WeatherProviderRegistry
import ru.hse.miem.miptweather.data.local.LocationDao
import ru.hse.miem.miptweather.data.local.WeatherDao
import ru.hse.miem.miptweather.data.local.WeatherEntity
import ru.hse.miem.miptweather.data.mapper.toEntity
import ru.hse.miem.miptweather.data.preferences.PreferencesManager
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import ru.hse.miem.miptweather.domain.util.Result
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val locationDao: LocationDao,
    private val providerRegistry: WeatherProviderRegistry,
    private val preferencesManager: PreferencesManager,
    private val json: Json
) : WeatherRepository {

    private companion object {
        val CACHE_TTL = 1.hours
        const val TAG = "WeatherRepository"
    }

    override fun getWeatherData(
        location: Location,
        dateRange: DateRange,
        forceRefresh: Boolean
    ): Flow<Result<WeatherData>> = flow {
        emit(Result.Loading)
        Log.d(TAG, "getWeatherData: loc=$location, range=$dateRange, refresh=$forceRefresh")

        if (!location.isValid()) {
            emit(Result.Error(IllegalArgumentException("Invalid location coordinates")))
            return@flow
        }
        if (!dateRange.isValid() || dateRange.days > DateRange.MAX_RANGE_DAYS) {
            emit(Result.Error(IllegalArgumentException("Invalid date range (max ${DateRange.MAX_RANGE_DAYS} days)")))
            return@flow
        }

        val locationId = getOrCreateLocationId(location)
        val preferredProviderId = preferencesManager.getPreferredProviderId()
        Log.d(TAG, "Using locationId=$locationId, preferredProvider=$preferredProviderId")

        var cachedData: WeatherData? = null
        if (!forceRefresh) {
            cachedData = fetchFromCache(locationId, dateRange, preferredProviderId)
            if (cachedData != null) {
                Log.d(TAG, "Cache hit for $preferredProviderId")
                emit(Result.Success(cachedData))
                if (isCacheValid(cachedData.timestamp)) {
                    Log.d(TAG, "Cache is fresh. Data loading finished.")
                    return@flow
                } else {
                    Log.d(TAG, "Cache is stale. Will fetch fresh data.")
                }
            } else {
                Log.d(TAG, "Cache miss for $preferredProviderId.")
            }
        } else {
            Log.d(TAG, "Force refresh requested. Skipping cache check.")
        }

        try {
            val result = fetchFromNetworkOrFallback(location, dateRange, locationId, preferredProviderId)
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception during network fetch", e)
            emit(Result.Error(e))
        }

    }.catch { e ->
        Log.e(TAG, "Flow exception in getWeatherData", e)
        emit(Result.Error(e))
    }

    private suspend fun fetchFromNetworkOrFallback(
        location: Location,
        dateRange: DateRange,
        locationId: Long,
        preferredProviderId: String
    ): Result<WeatherData> {
        val primaryProvider = providerRegistry.getProvider(preferredProviderId)
            ?: providerRegistry.getCurrentProviders().firstOrNull { it.id == preferredProviderId }

        if (primaryProvider != null) {
            Log.d(TAG, "Attempting fetch from primary provider: ${primaryProvider.id}")
            when (val result = fetchAndCache(primaryProvider, location, dateRange, locationId)) {
                is Result.Success -> return result
                is Result.Error -> {
                    Log.w(TAG, "Primary provider ${primaryProvider.id} failed: ${result.exception.message}")
                }
                is Result.Loading -> { /* Игнорируем */ }
            }
        } else {
            Log.w(TAG, "Preferred provider '$preferredProviderId' not found.")
        }

        val fallbackProvider = findFallbackProvider(preferredProviderId)
        if (fallbackProvider != null) {
            Log.w(TAG, "Falling back to provider: ${fallbackProvider.id}")
            return fetchAndCache(fallbackProvider, location, dateRange, locationId)
        } else {
            Log.e(TAG, "No fallback provider available.")
            return Result.Error(Exception("No suitable weather provider available (preferred '$preferredProviderId' not found, no fallback)."))
        }
    }

    private suspend fun fetchAndCache(
        provider: WeatherProvider,
        location: Location,
        dateRange: DateRange,
        locationId: Long
    ): Result<WeatherData> {
        return when (val result = provider.getWeatherData(location, dateRange)) {
            is Result.Success -> {
                Log.d(TAG, "Successfully fetched data from ${provider.id}")
                updateLocationDetailsIfNeeded(locationId, result.data.location)
                saveToCache(locationId, result.data)
                result
            }
            is Result.Error -> {
                Log.e(TAG, "Error fetching from ${provider.id}: ${result.exception.message}")
                result
            }
            is Result.Loading -> result
        }
    }


    private fun findFallbackProvider(failedProviderId: String): WeatherProvider? =
        providerRegistry.getCurrentProviders()
            .filter { it.id != failedProviderId }
            .maxByOrNull { it.priority }

    override fun getAvailableProviders(): Flow<List<String>> =
        providerRegistry.getProviderIds()

    override suspend fun setPreferredProvider(providerId: String) {
        if (providerRegistry.getCurrentProviders().any { it.id == providerId }) {
            Log.d(TAG, "Setting preferred provider to: $providerId")
            preferencesManager.setPreferredProvider(providerId)
        } else {
            Log.e(TAG, "Attempted to set unknown provider '$providerId' as preferred.")
        }
    }

    override fun getPreferredProvider(): Flow<String> =
        preferencesManager.getPreferredProviderIdFlow()

    private suspend fun getOrCreateLocationId(location: Location): Long {
        val existingLocation = locationDao.findLocationByCoordinates(location.latitude, location.longitude)
        return existingLocation?.id ?: run {
            Log.d(TAG, "Creating new location entry for: ${location.latitude}, ${location.longitude}")
            val newEntity = location.toEntity()
            locationDao.insertLocation(newEntity)
        }
    }

    private suspend fun fetchFromCache(
        locationId: Long,
        dateRange: DateRange,
        provider: String
    ): WeatherData? {
        Log.v(TAG, "fetchFromCache: locId=$locationId, provider=$provider, range=$dateRange")
        val enclosingFlow = weatherDao.getEnclosingWeatherData(locationId, provider, dateRange.startDate, dateRange.endDate)
        val enclosing = enclosingFlow.first()

        if (enclosing != null) {
            Log.v(TAG, "Found enclosing cache (id=${enclosing.id}, ts=${enclosing.timestamp})")
            return decodeWeatherData(enclosing.weatherDataJson)
        }

        val overlappingFlow = weatherDao.getOverlappingWeatherData(locationId, provider, dateRange.startDate, dateRange.endDate)
        val overlapping = overlappingFlow.first()

        if (overlapping != null) {
            Log.v(TAG, "Found overlapping cache (id=${overlapping.id}, ts=${overlapping.timestamp})")
            return decodeWeatherData(overlapping.weatherDataJson)
        }

        Log.v(TAG, "No suitable cache found.")
        return null
    }

    private fun decodeWeatherData(jsonString: String): WeatherData? {
        return try {
            json.decodeFromString<WeatherData>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode cached WeatherData", e)
            null
        }
    }

    private suspend fun saveToCache(locationId: Long, weatherData: WeatherData) {
        Log.d(TAG, "Saving weather data to cache: locId=$locationId, provider=${weatherData.provider}, range=${weatherData.dateRange}")
        try {
            val weatherEntity = WeatherEntity(
                locationId = locationId,
                startDate = weatherData.dateRange.startDate,
                endDate = weatherData.dateRange.endDate,
                provider = weatherData.provider,
                timestamp = weatherData.timestamp,
                weatherDataJson = json.encodeToString(weatherData)
            )
            weatherDao.insertWeatherData(weatherEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode or save WeatherData to cache", e)
        }
    }

    private fun isCacheValid(timestamp: LocalDateTime): Boolean {
        val now = Clock.System.now()
        val timestampInstant = timestamp.toInstant(TimeZone.currentSystemDefault())
        val duration = now - timestampInstant
        return duration < CACHE_TTL
    }

    private suspend fun updateLocationDetailsIfNeeded(locationId: Long, locationFromApi: Location) {
        val currentDetails = locationDao.getLocationById(locationId) ?: return

        val nameChanged = locationFromApi.name.isNotBlank() && locationFromApi.name != currentDetails.name
        val countryChanged = locationFromApi.countryCode.isNotBlank() && locationFromApi.countryCode != currentDetails.countryCode
        val timezoneChanged = locationFromApi.timezone.isNotBlank() && locationFromApi.timezone != currentDetails.timezone

        if (nameChanged || countryChanged || timezoneChanged) {
            Log.d(TAG, "Updating location details for id $locationId from API data: $locationFromApi")
            val updatedEntity = currentDetails.copy(
                name = if (nameChanged) locationFromApi.name else currentDetails.name,
                countryCode = if (countryChanged) locationFromApi.countryCode else currentDetails.countryCode,
                timezone = if (timezoneChanged) locationFromApi.timezone else currentDetails.timezone
            )
            locationDao.updateLocation(updatedEntity)
        }
    }
}