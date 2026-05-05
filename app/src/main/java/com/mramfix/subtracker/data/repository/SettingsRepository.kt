package com.mramfix.subtracker.data.repository

import com.mramfix.subtracker.data.settings.SettingsDataStore
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {
    val settings: Flow<AppSettings> = dataStore.settings

    suspend fun updateTheme(mode: AppThemeMode) = dataStore.updateTheme(mode)
    suspend fun updateNotificationsEnabled(enabled: Boolean) = dataStore.updateNotificationsEnabled(enabled)
    suspend fun updateNotificationLead(lead: NotificationLead) = dataStore.updateNotificationLead(lead)
    suspend fun updateBaseCurrency(currency: CurrencyCode) = dataStore.updateBaseCurrency(currency)
    suspend fun updateSalaryDay(day: Int) = dataStore.updateSalaryDay(day)
    suspend fun updateAdvanceDay(day: Int) = dataStore.updateAdvanceDay(day)
    suspend fun updateAutoGoogleSyncEnabled(enabled: Boolean) = dataStore.updateAutoGoogleSyncEnabled(enabled)
    suspend fun replace(settings: AppSettings) = dataStore.replace(settings)
}
