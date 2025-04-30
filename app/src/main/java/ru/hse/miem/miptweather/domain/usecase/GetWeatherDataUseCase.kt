package ru.hse.miem.miptweather.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import ru.hse.miem.miptweather.domain.util.Result
import javax.inject.Inject

class GetWeatherDataUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(
        location: Location,
        dateRange: DateRange,
        forceRefresh: Boolean = false
    ): Flow<Result<WeatherData>> =
        weatherRepository.getWeatherData(location, dateRange, forceRefresh)
}