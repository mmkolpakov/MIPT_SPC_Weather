package ru.hse.miem.miptweather.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["latitude", "longitude"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val countryCode: String,
    val timezone: String
)