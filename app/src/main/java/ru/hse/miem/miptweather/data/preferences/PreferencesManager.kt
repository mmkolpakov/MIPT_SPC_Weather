package ru.hse.miem.miptweather.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val PREFERRED_PROVIDER_ID = stringPreferencesKey("preferred_provider_id")
        const val DEFAULT_PROVIDER_ID = "open-meteo"
    }

    suspend fun setPreferredProvider(providerId: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_PROVIDER_ID] = providerId
        }
    }

    fun getPreferredProviderIdFlow(): Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PREFERRED_PROVIDER_ID] ?: DEFAULT_PROVIDER_ID
        }
        .distinctUntilChanged()

    suspend fun getPreferredProviderId(): String =
        getPreferredProviderIdFlow().first()
}