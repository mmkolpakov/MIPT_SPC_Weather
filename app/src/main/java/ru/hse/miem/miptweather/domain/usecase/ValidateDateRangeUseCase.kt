package ru.hse.miem.miptweather.domain.usecase

import javax.inject.Inject
import kotlinx.datetime.LocalDate
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.util.Result

class ValidateDateRangeUseCase @Inject constructor() {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Result<DateRange> {
        val dateRange = DateRange(startDate, endDate)

        return when {
            !dateRange.isValid() ->
                Result.Error(IllegalArgumentException("Дата окончания должна быть не раньше даты начала"))
            dateRange.days > DateRange.MAX_RANGE_DAYS ->
                Result.Error(IllegalArgumentException("Диапазон дат не может превышать ${DateRange.MAX_RANGE_DAYS} дней"))
            else ->
                Result.Success(dateRange)
        }
    }
}