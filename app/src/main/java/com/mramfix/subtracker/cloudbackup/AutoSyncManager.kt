package com.mramfix.subtracker.cloudbackup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mramfix.subtracker.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

class AutoSyncManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    fun hasSignedInAccount(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

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

    private sealed class TokenResult {
        data class Success(val accessToken: String) : TokenResult()
        data class Error(val message: String) : TokenResult()
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
