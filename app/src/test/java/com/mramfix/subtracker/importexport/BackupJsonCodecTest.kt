package com.mramfix.subtracker.importexport

import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupJsonCodecTest {
    @Test
    fun exportsAndImportsBackupPayload() {
        val payload = BackupPayload(
            settings = AppSettings(baseCurrency = CurrencyCode.USD),
            subscriptions = listOf(subscription(name = "Netflix"))
        )

        val text = BackupJsonCodec.encode(payload)
        val decoded = BackupJsonCodec.decode(text)

        assertTrue(decoded is ImportResult.Success)
        decoded as ImportResult.Success
        assertEquals(CurrencyCode.USD, decoded.payload.settings.baseCurrency)
        assertEquals("Netflix", decoded.payload.subscriptions.single().name)
    }

    @Test
    fun importRejectsInvalidSubscription() {
        val payload = BackupPayload(
            settings = AppSettings(),
            subscriptions = listOf(subscription(name = "", cost = 0.0))
        )

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))

        assertTrue(decoded is ImportResult.Error)
    }

    private fun subscription(name: String, cost: Double = 12.5): Subscription {
        return Subscription(
            id = 10,
            name = name,
            cost = cost,
            currency = CurrencyCode.EUR,
            status = SubscriptionStatus.ACTIVE,
            nextPaymentEpochDay = LocalDate.of(2026, 5, 15).toEpochDay(),
            billingRule = BillingRule(BillingPeriodType.MONTHLY_CALENDAR_DAY, calendarDay = 15),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
    }
}
