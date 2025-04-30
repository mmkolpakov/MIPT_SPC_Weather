package ru.hse.miem.miptweather.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.hse.miem.miptweather.data.repository.LocationRepositoryImpl
import ru.hse.miem.miptweather.data.repository.SettingsRepositoryImpl
import ru.hse.miem.miptweather.data.repository.WeatherRepositoryImpl
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import ru.hse.miem.miptweather.domain.repository.SettingsRepository
import ru.hse.miem.miptweather.domain.repository.WeatherRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}