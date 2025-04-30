package ru.hse.miem.miptweather.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.miptweather.domain.model.UnitPreferences

interface SettingsRepository {
    fun getUnitPreferences(): Flow<UnitPreferences>
    suspend fun saveUnitPreferences(preferences: UnitPreferences)
}