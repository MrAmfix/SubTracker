package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationLead: NotificationLead = NotificationLead.ONE_DAY,
    val baseCurrency: CurrencyCode = CurrencyCode.RUB,
    val salaryDay: Int = 5,
    val advanceDay: Int = 20,
    val autoGoogleSyncEnabled: Boolean = false
)
