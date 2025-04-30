package ru.hse.miem.miptweather.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Entity(
    tableName = "weather_data",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId"), Index("locationId", "provider", "startDate", "endDate")]
)
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val provider: String,
    val timestamp: LocalDateTime,
    val weatherDataJson: String
)