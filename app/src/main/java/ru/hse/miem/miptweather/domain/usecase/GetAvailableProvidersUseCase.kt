package ru.hse.miem.miptweather.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetAvailableProvidersUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(): Flow<List<String>> = weatherRepository.getAvailableProviders()
}