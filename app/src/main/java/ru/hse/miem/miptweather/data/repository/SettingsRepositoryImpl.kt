package ru.hse.miem.miptweather.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.hse.miem.miptweather.domain.model.UnitPreferences
import ru.hse.miem.miptweather.domain.repository.SettingsRepository
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
    }

    override fun getUnitPreferences(): Flow<UnitPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val tempUnit = TemperatureUnit.valueOf(
                preferences[Keys.TEMPERATURE_UNIT] ?: TemperatureUnit.CELSIUS.name
            )
            val speedUnit = SpeedUnit.valueOf(
                preferences[Keys.SPEED_UNIT] ?: SpeedUnit.METERS_PER_SECOND.name
            )
            val pressureUnit = PressureUnit.valueOf(
                preferences[Keys.PRESSURE_UNIT] ?: PressureUnit.HECTOPASCALS.name
            )
            UnitPreferences(
                temperatureUnit = tempUnit,
                speedUnit = speedUnit,
                pressureUnit = pressureUnit
            )
        }

    override suspend fun saveUnitPreferences(preferences: UnitPreferences) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[Keys.TEMPERATURE_UNIT] = preferences.temperatureUnit.name
            mutablePreferences[Keys.SPEED_UNIT] = preferences.speedUnit.name
            mutablePreferences[Keys.PRESSURE_UNIT] = preferences.pressureUnit.name
        }
    }
}