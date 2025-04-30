package ru.hse.miem.miptweather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WeatherEntity::class, LocationEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun locationDao(): LocationDao
}