package com.mramfix.subtracker.importexport

import com.mramfix.subtracker.domain.model.Subscription
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

object BackupJsonCodec {
    const val CURRENT_VERSION = 1

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(payload: BackupPayload): String = json.encodeToString(payload.copy(version = CURRENT_VERSION))

    fun decode(text: String): ImportResult {
        return try {
            val payload = json.decodeFromString<BackupPayload>(text)
            val validationError = validate(payload)
            if (validationError == null) ImportResult.Success(payload) else ImportResult.Error(validationError)
        } catch (e: SerializationException) {
            ImportResult.Error("JSON не соответствует формату SubTracker")
        } catch (e: IllegalArgumentException) {
            ImportResult.Error("Некорректные значения в JSON")
        }
    }

    fun validate(payload: BackupPayload): String? {
        if (payload.version > CURRENT_VERSION) return "Файл создан более новой версией приложения"
        payload.subscriptions.forEachIndexed { index, subscription ->
            validateSubscription(subscription)?.let { return "Подписка #${index + 1}: $it" }
        }
        return null
    }

    private fun validateSubscription(subscription: Subscription): String? {
        if (subscription.name.isBlank()) return "название не может быть пустым"
        if (!subscription.cost.isFinite() || subscription.cost <= 0.0) return "стоимость должна быть больше 0"
        if (!subscription.billingRule.validate()) return "правило оплаты некорректно"
        runCatching { LocalDate.ofEpochDay(subscription.nextPaymentEpochDay) }
            .getOrElse { return "дата оплаты некорректна" }
        if (subscription.createdAtEpochMillis <= 0L) return "дата создания некорректна"
        if (subscription.updatedAtEpochMillis <= 0L) return "дата обновления некорректна"
        return null
    }
}
