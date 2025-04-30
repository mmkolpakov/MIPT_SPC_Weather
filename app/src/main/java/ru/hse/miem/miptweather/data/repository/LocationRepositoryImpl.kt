package ru.hse.miem.miptweather.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.hse.miem.miptweather.data.local.LocationDao
import ru.hse.miem.miptweather.data.mapper.toDomain
import ru.hse.miem.miptweather.data.mapper.toEntity
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    override fun getSavedLocations(): Flow<List<Location>> =
        locationDao.getAllLocations().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun findSavedLocationByCoords(latitude: Double, longitude: Double): Location? =
        locationDao.findLocationByCoordinates(latitude, longitude)?.toDomain()

    override suspend fun saveLocation(location: Location): Long =
        locationDao.insertLocation(location.toEntity())

    override suspend fun deleteLocation(location: Location) {
        val entityToDelete = locationDao.findLocationByCoordinates(
            location.latitude,
            location.longitude
        )
        entityToDelete?.let {
            locationDao.deleteLocation(it)
        }
    }

    override suspend fun updateLocation(location: Location) {
        val existingEntity = locationDao.findLocationByCoordinates(location.latitude, location.longitude)
        if (existingEntity != null) {
            val updatedEntity = location.toEntity().copy(id = existingEntity.id)
            locationDao.updateLocation(updatedEntity)
        } else {
            saveLocation(location)
        }
    }

    override suspend fun getLocationById(id: Long): Location? =
        locationDao.getLocationById(id)?.toDomain()

}