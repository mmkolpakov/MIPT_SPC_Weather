package ru.hse.miem.miptweather.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(locationEntity: LocationEntity): Long

    @Query("""
        SELECT * FROM locations
        WHERE ABS(latitude - :lat) < 0.0001
        AND ABS(longitude - :lon) < 0.0001
        LIMIT 1
    """)
    suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Long): LocationEntity?

    @Delete
    suspend fun deleteLocation(locationEntity: LocationEntity)

    @Update
    suspend fun updateLocation(locationEntity: LocationEntity)

    @Transaction
    suspend fun updateLocationDetails(id: Long, name: String?, countryCode: String?, timezone: String?) {
        val current = getLocationById(id)
        if (current != null) {
            val updated = current.copy(
                name = name?.takeIf { it.isNotBlank() } ?: current.name,
                countryCode = countryCode?.takeIf { it.isNotBlank() } ?: current.countryCode,
                timezone = timezone?.takeIf { it.isNotBlank() } ?: current.timezone
            )
            if (updated != current) {
                updateLocation(updated)
            }
        }
    }
}