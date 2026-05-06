package com.mramfix.subtracker.cloudbackup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mramfix.subtracker.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class AutoSyncManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val syncMutex = Mutex()
    private val _syncErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncErrors: SharedFlow<String> = _syncErrors.asSharedFlow()

    @Volatile
    private var syncPending = false

    fun hasSignedInAccount(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    fun requestSyncIfEnabled() {
        scope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.autoGoogleSyncEnabled) return@launch
            syncPending = true
            if (syncMutex.isLocked) return@launch
            syncMutex.withLock {
                while (syncPending) {
                    syncPending = false
                    when (val result = exportWithRetry()) {
                        AutoSyncResult.Success -> Unit
                        is AutoSyncResult.Error -> _syncErrors.emit("Проблема с синхронизацией с Google Drive: ${result.message}")
                    }
                }
            }
        }
    }

    suspend fun syncIfEnabled(): AutoSyncResult? {
        val settings = settingsRepository.settings.first()
        if (!settings.autoGoogleSyncEnabled) return null
        return exportWithLastAccount()
    }

    suspend fun exportWithLastAccount(): AutoSyncResult {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return AutoSyncResult.Error("Google Account не выбран")
        return exportForAccount(account)
    }

    suspend fun exportForAccount(account: GoogleSignInAccount): AutoSyncResult = withContext(Dispatchers.IO) {
        val token = when (val tokenResult = getDriveToken(account)) {
            is TokenResult.Success -> tokenResult.accessToken
            is TokenResult.Error -> return@withContext AutoSyncResult.Error(tokenResult.message)
        }
        googleDriveBackupRepository.exportToGoogleDrive(token).fold(
            onSuccess = { AutoSyncResult.Success },
            onFailure = { AutoSyncResult.Error(it.toUserMessage("Не удалось экспортировать")) }
        )
    }

    suspend fun importForAccount(account: GoogleSignInAccount): CloudImportResult = withContext(Dispatchers.IO) {
        val token = when (val tokenResult = getDriveToken(account)) {
            is TokenResult.Success -> tokenResult.accessToken
            is TokenResult.Error -> return@withContext CloudImportResult.Error(tokenResult.message)
        }
        googleDriveBackupRepository.importFromGoogleDrive(token)
    }

    private fun getDriveToken(account: GoogleSignInAccount): TokenResult {
        val androidAccount = account.account ?: return TokenResult.Error("Не удалось получить Google Account")
        return try {
            TokenResult.Success(
                GoogleAuthUtil.getToken(
                    context,
                    androidAccount,
                    "oauth2:${GoogleDriveBackupRepository.DRIVE_APPDATA_SCOPE}"
                )
            )
        } catch (e: UserRecoverableAuthException) {
            TokenResult.Error("Нужно разрешение Google Drive. Выполните ручной экспорт через Google и подтвердите доступ")
        } catch (e: IOException) {
            TokenResult.Error("Нет подключения к Google")
        } catch (e: GoogleAuthException) {
            TokenResult.Error("Не удалось авторизоваться в Google: ${e.message ?: e::class.simpleName}")
        } catch (e: SecurityException) {
            TokenResult.Error("Google Drive permission не выдан")
        }
    }

    private suspend fun exportWithRetry(): AutoSyncResult {
        var lastError: AutoSyncResult.Error? = null
        repeat(MAX_AUTO_SYNC_ATTEMPTS) { attempt ->
            when (val result = exportWithLastAccount()) {
                AutoSyncResult.Success -> return AutoSyncResult.Success
                is AutoSyncResult.Error -> lastError = result
            }
            if (attempt < MAX_AUTO_SYNC_ATTEMPTS - 1) delay(AUTO_SYNC_RETRY_DELAY_MILLIS)
        }
        return lastError ?: AutoSyncResult.Error("Не удалось сохранить резервную копию")
    }

    private sealed class TokenResult {
        data class Success(val accessToken: String) : TokenResult()
        data class Error(val message: String) : TokenResult()
    }

    private companion object {
        const val MAX_AUTO_SYNC_ATTEMPTS = 4
        const val AUTO_SYNC_RETRY_DELAY_MILLIS = 5_000L
    }
}

sealed class AutoSyncResult {
    data object Success : AutoSyncResult()
    data class Error(val message: String) : AutoSyncResult()
}

fun Throwable.toUserMessage(prefix: String): String {
    return if (this is DriveApiException) {
        "$prefix: HTTP $code, $shortBody"
    } else {
        "$prefix: ${this::class.simpleName}${message?.let { ": $it" }.orEmpty()}"
    }
}
