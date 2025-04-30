package ru.hse.miem.miptweather.service

import ru.hse.miem.miptweather.data.api.OpenMeteoProvider
import ru.hse.miem.miptweather.data.api.OpenWeatherMapProvider
import ru.hse.miem.miptweather.data.api.WeatherApiProvider
import ru.hse.miem.miptweather.data.api.WeatherProvider
import ru.hse.miem.miptweather.data.api.WeatherProviderRegistry
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WeatherProviderService @Inject constructor(
    private val providerRegistry: WeatherProviderRegistry,
    private val openMeteoProvider: Provider<OpenMeteoProvider>,
    private val openWeatherMapProvider: Provider<OpenWeatherMapProvider>,
    private val weatherApiProvider: Provider<WeatherApiProvider>
) {
    private var areProvidersRegistered = false

    @Synchronized
    fun registerAllProvidersIfNeeded() {
        if (areProvidersRegistered) {
            return
        }

        registerProviderInternal(openMeteoProvider.get())
        registerProviderInternal(openWeatherMapProvider.get())
        registerProviderInternal(weatherApiProvider.get())

        areProvidersRegistered = true
    }

    private fun registerProviderInternal(provider: WeatherProvider) {
        try {
            providerRegistry.registerProvider(provider)
        } catch (e: Exception) {
        }
    }
}