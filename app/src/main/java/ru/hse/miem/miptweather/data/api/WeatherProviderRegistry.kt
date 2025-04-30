package ru.hse.miem.miptweather.data.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherProviderRegistry @Inject constructor() {
    private val _providers = MutableStateFlow<List<WeatherProvider>>(emptyList())
    val providers: Flow<List<WeatherProvider>> = _providers.asStateFlow()

    fun registerProvider(provider: WeatherProvider) {
        _providers.update { currentProviders ->
            if (currentProviders.any { it.id == provider.id }) {
                currentProviders
            } else {
                (currentProviders + provider).sortedByDescending { it.priority }
            }
        }
    }

    fun unregisterProvider(providerId: String) {
        _providers.update { currentProviders ->
            currentProviders.filterNot { it.id == providerId }
        }
    }

    fun getProvider(providerId: String): WeatherProvider? =
        _providers.value.find { it.id == providerId }

    fun getCurrentProviders(): List<WeatherProvider> = _providers.value

    fun getProviderIds(): Flow<List<String>> = providers.map { providerList ->
        providerList.map { it.id }
    }
}