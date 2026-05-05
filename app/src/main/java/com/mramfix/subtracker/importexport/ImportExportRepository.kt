package com.mramfix.subtracker.importexport

import com.mramfix.subtracker.data.repository.SettingsRepository
import com.mramfix.subtracker.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first

class ImportExportRepository(
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun exportJson(): String {
        val payload = BackupPayload(
            settings = settingsRepository.settings.first(),
            subscriptions = subscriptionRepository.getAll()
        )
        return BackupJsonCodec.encode(payload)
    }

    suspend fun importJson(text: String): ImportResult {
        return when (val result = BackupJsonCodec.decode(text)) {
            is ImportResult.Error -> result
            is ImportResult.Success -> {
                subscriptionRepository.replaceAll(result.payload.subscriptions)
                settingsRepository.replace(result.payload.settings)
                result
            }
        }
    }
}
