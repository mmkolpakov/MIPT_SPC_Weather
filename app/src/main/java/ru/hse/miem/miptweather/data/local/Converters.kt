package ru.hse.miem.miptweather.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate = LocalDate.parse(dateString)

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): String = dateTime.toString()

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String): LocalDateTime = LocalDateTime.parse(dateTimeString)
}