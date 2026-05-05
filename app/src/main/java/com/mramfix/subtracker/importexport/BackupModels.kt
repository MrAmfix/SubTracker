package com.mramfix.subtracker.importexport

import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.Subscription
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = BackupJsonCodec.CURRENT_VERSION,
    val settings: AppSettings,
    val subscriptions: List<Subscription>
)

sealed class ImportResult {
    data class Success(val payload: BackupPayload) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
