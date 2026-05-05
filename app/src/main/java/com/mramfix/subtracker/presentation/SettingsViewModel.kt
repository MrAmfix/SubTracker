package com.mramfix.subtracker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mramfix.subtracker.SubTrackerApplication
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import com.mramfix.subtracker.importexport.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SubTrackerApplication).container
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(container.settingsRepository.settings, busy, message) { settings, isBusy, currentMessage ->
        SettingsUiState(settings, isBusy, currentMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateTheme(mode: AppThemeMode) = launchAndReschedule { container.settingsRepository.updateTheme(mode) }
    fun updateNotificationsEnabled(enabled: Boolean) = launchAndReschedule { container.settingsRepository.updateNotificationsEnabled(enabled) }
    fun updateNotificationLead(lead: NotificationLead) = launchAndReschedule { container.settingsRepository.updateNotificationLead(lead) }
    fun updateBaseCurrency(currency: CurrencyCode) = launchAndReschedule { container.settingsRepository.updateBaseCurrency(currency) }

    fun refreshRates() {
        viewModelScope.launch {
            busy.value = true
            val result = container.currencyRepository.refreshRates()
            busy.value = false
            message.value = result.fold(
                onSuccess = { "Курсы валют обновлены" },
                onFailure = { "Не удалось обновить курсы валют" }
            )
        }
    }

    suspend fun exportJson(): String = container.importExportRepository.exportJson()

    fun importJson(text: String) {
        viewModelScope.launch {
            busy.value = true
            when (val result = container.importExportRepository.importJson(text)) {
                is ImportResult.Success -> {
                    rescheduleNotifications()
                    message.value = "Импорт завершен"
                }
                is ImportResult.Error -> message.value = result.message
            }
            busy.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun launchAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            rescheduleNotifications()
        }
    }

    private suspend fun rescheduleNotifications() {
        container.notificationScheduler.rescheduleAll(
            container.subscriptionRepository.getAll(),
            container.settingsRepository.settings.first()
        )
    }
}
