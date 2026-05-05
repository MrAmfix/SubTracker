package com.mramfix.subtracker.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[Keys.themeMode]?.let(AppThemeMode::valueOf) ?: AppThemeMode.SYSTEM,
            notificationsEnabled = preferences[Keys.notificationsEnabled] ?: true,
            notificationLead = NotificationLead.entries.firstOrNull {
                it.daysBefore == (preferences[Keys.notificationLeadDays] ?: NotificationLead.ONE_DAY.daysBefore)
            } ?: NotificationLead.ONE_DAY,
            baseCurrency = preferences[Keys.baseCurrency]?.let(CurrencyCode::valueOf) ?: CurrencyCode.RUB,
            salaryDay = preferences[Keys.salaryDay]?.coerceIn(1, 31) ?: 5,
            advanceDay = preferences[Keys.advanceDay]?.coerceIn(1, 31) ?: 20,
            autoGoogleSyncEnabled = preferences[Keys.autoGoogleSyncEnabled] ?: false
        )
    }

    suspend fun updateTheme(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.notificationsEnabled] = enabled }
    }

    suspend fun updateNotificationLead(lead: NotificationLead) {
        context.settingsDataStore.edit { it[Keys.notificationLeadDays] = lead.daysBefore }
    }

    suspend fun updateBaseCurrency(currency: CurrencyCode) {
        context.settingsDataStore.edit { it[Keys.baseCurrency] = currency.name }
    }

    suspend fun updateSalaryDay(day: Int) {
        context.settingsDataStore.edit { it[Keys.salaryDay] = day.coerceIn(1, 31) }
    }

    suspend fun updateAdvanceDay(day: Int) {
        context.settingsDataStore.edit { it[Keys.advanceDay] = day.coerceIn(1, 31) }
    }

    suspend fun updateAutoGoogleSyncEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoGoogleSyncEnabled] = enabled }
    }

    suspend fun replace(settings: AppSettings) {
        context.settingsDataStore.edit {
            it[Keys.themeMode] = settings.themeMode.name
            it[Keys.notificationsEnabled] = settings.notificationsEnabled
            it[Keys.notificationLeadDays] = settings.notificationLead.daysBefore
            it[Keys.baseCurrency] = settings.baseCurrency.name
            it[Keys.salaryDay] = settings.salaryDay.coerceIn(1, 31)
            it[Keys.advanceDay] = settings.advanceDay.coerceIn(1, 31)
            it[Keys.autoGoogleSyncEnabled] = settings.autoGoogleSyncEnabled
        }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val notificationLeadDays = intPreferencesKey("notification_lead_days")
        val baseCurrency = stringPreferencesKey("base_currency")
        val salaryDay = intPreferencesKey("salary_day")
        val advanceDay = intPreferencesKey("advance_day")
        val autoGoogleSyncEnabled = booleanPreferencesKey("auto_google_sync_enabled")
    }
}
