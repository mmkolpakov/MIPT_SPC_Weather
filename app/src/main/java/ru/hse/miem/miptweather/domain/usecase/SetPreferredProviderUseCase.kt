package ru.hse.miem.miptweather.domain.usecase

import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import javax.inject.Inject

class SetPreferredProviderUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(providerId: String) {
        weatherRepository.setPreferredProvider(providerId)
    }
}