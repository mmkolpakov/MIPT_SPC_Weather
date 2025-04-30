package ru.hse.miem.miptweather.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.miptweather.domain.model.Location

interface LocationRepository {
    fun getSavedLocations(): Flow<List<Location>>
    suspend fun saveLocation(location: Location): Long
    suspend fun deleteLocation(location: Location)
    suspend fun getLocationById(id: Long): Location?
    suspend fun findSavedLocationByCoords(latitude: Double, longitude: Double): Location?
    suspend fun updateLocation(location: Location)
}