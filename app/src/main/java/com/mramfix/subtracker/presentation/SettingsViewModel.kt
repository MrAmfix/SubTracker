package com.mramfix.subtracker.presentation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mramfix.subtracker.SubTrackerApplication
import com.mramfix.subtracker.cloudbackup.AutoSyncResult
import com.mramfix.subtracker.cloudbackup.CloudImportResult
import com.mramfix.subtracker.cloudbackup.GoogleDriveBackupRepository
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import com.mramfix.subtracker.importexport.ImportResult
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: String? = null,
    val googleSignedIn: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SubTrackerApplication).container
    private val appContext = application.applicationContext
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val googleSignedIn = MutableStateFlow(container.autoSyncManager.hasSignedInAccount())

    val uiState = combine(
        container.settingsRepository.settings,
        busy,
        message,
        googleSignedIn
    ) { settings, isBusy, currentMessage, signedIn ->
        SettingsUiState(settings, isBusy, currentMessage, signedIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateTheme(mode: AppThemeMode) = launchAndReschedule { container.settingsRepository.updateTheme(mode) }
    fun updateNotificationsEnabled(enabled: Boolean) = launchAndReschedule { container.settingsRepository.updateNotificationsEnabled(enabled) }
    fun updateNotificationLead(lead: NotificationLead) = launchAndReschedule { container.settingsRepository.updateNotificationLead(lead) }
    fun updateBaseCurrency(currency: CurrencyCode) = launchAndReschedule { container.settingsRepository.updateBaseCurrency(currency) }

    fun updateAutoGoogleSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !container.autoSyncManager.hasSignedInAccount()) {
                googleSignedIn.value = false
                message.value = "Сначала выполните экспорт или импорт через Google"
                return@launch
            }
            container.settingsRepository.updateAutoGoogleSyncEnabled(enabled)
            message.value = if (enabled) "Автоматическая синхронизация включена" else "Автоматическая синхронизация выключена"
        }
    }

    fun googleSignInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GoogleDriveBackupRepository.DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(appContext, options).signInIntent
    }

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

    fun exportToGoogle(data: Intent?) {
        runGoogleDriveAction(data) { account ->
            when (val result = container.autoSyncManager.exportForAccount(account)) {
                AutoSyncResult.Success -> "Резервная копия сохранена в Google Drive"
                is AutoSyncResult.Error -> result.message
            }
        }
    }

    fun importFromGoogle(data: Intent?) {
        runGoogleDriveAction(data) { account ->
            when (val result = container.autoSyncManager.importForAccount(account)) {
                CloudImportResult.Success -> {
                    rescheduleNotifications()
                    "Импорт из Google Drive завершен"
                }
                is CloudImportResult.Error -> result.message
            }
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

    private fun runGoogleDriveAction(
        data: Intent?,
        action: suspend (GoogleSignInAccount) -> String
    ) {
        viewModelScope.launch {
            busy.value = true
            val account = parseGoogleAccount(data)
            if (account == null) {
                busy.value = false
                message.value = "Google Account не выбран"
                return@launch
            }
            googleSignedIn.value = true
            message.value = action(account)
            busy.value = false
        }
    }

    private fun parseGoogleAccount(data: Intent?): GoogleSignInAccount? {
        return runCatching { GoogleSignIn.getSignedInAccountFromIntent(data).result }.getOrNull()
    }

    private suspend fun rescheduleNotifications() {
        container.notificationScheduler.rescheduleAll(
            container.subscriptionRepository.getAll(),
            container.settingsRepository.settings.first()
        )
    }
}
