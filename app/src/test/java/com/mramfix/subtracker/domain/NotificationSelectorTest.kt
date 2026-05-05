package com.mramfix.subtracker.domain

import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.model.SubscriptionStatus
import com.mramfix.subtracker.domain.usecase.NotificationSelector
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NotificationSelectorTest {
    @Test
    fun selectsActiveSubscriptionsAtConfiguredLeadDate() {
        val today = LocalDate.of(2026, 5, 5)
        val active = subscription(1, today.plusDays(3), SubscriptionStatus.ACTIVE)
        val inactive = subscription(2, today.plusDays(3), SubscriptionStatus.INACTIVE)
        val later = subscription(3, today.plusDays(7), SubscriptionStatus.ACTIVE)

        val selected = NotificationSelector.dueForNotification(
            listOf(active, inactive, later),
            AppSettings(notificationLead = NotificationLead.THREE_DAYS),
            today
        )

        assertEquals(listOf(active), selected)
    }

    private fun subscription(id: Long, date: LocalDate, status: SubscriptionStatus): Subscription {
        return Subscription(
            id = id,
            name = "Test $id",
            cost = 10.0,
            currency = CurrencyCode.RUB,
            status = status,
            nextPaymentEpochDay = date.toEpochDay(),
            billingRule = BillingRule(BillingPeriodType.MONTHLY_FIXED, intervalDays = 30),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
    }
}
