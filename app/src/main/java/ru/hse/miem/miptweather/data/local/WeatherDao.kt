package ru.hse.miem.miptweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherEntity: WeatherEntity): Long

    @Query("""
        SELECT * FROM weather_data
        WHERE locationId = :locationId
        AND provider = :provider
        AND startDate <= :startDate
        AND endDate >= :endDate
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    fun getEnclosingWeatherData(
        locationId: Long,
        provider: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<WeatherEntity?>

    @Query("""
        SELECT * FROM weather_data
        WHERE locationId = :locationId
        AND provider = :provider
        AND startDate <= :endDate
        AND endDate >= :startDate
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    fun getOverlappingWeatherData(
        locationId: Long,
        provider: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<WeatherEntity?>


    @Query("""
        SELECT * FROM weather_data
        WHERE locationId = :locationId
        AND timestamp >= :minTimestamp
        ORDER BY timestamp DESC
    """)
    fun getRecentWeatherData(
        locationId: Long,
        minTimestamp: LocalDateTime
    ): Flow<List<WeatherEntity>>

    @Query("DELETE FROM weather_data WHERE timestamp < :olderThan")
    suspend fun deleteOldData(olderThan: LocalDateTime)
}