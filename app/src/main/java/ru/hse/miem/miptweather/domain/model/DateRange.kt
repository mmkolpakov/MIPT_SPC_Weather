package ru.hse.miem.miptweather.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import kotlinx.serialization.Serializable

@Serializable
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun isValid(): Boolean = startDate <= endDate

    val days: Int get() = if (isValid()) startDate.until(endDate, DateTimeUnit.DAY) + 1 else 0

    companion object {
        const val MAX_RANGE_DAYS = 31
    }
}