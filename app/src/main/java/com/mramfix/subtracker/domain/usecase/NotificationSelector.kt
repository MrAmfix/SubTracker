package com.mramfix.subtracker.domain.usecase

import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.Subscription
import java.time.LocalDate

object NotificationSelector {
    fun dueForNotification(
        subscriptions: List<Subscription>,
        settings: AppSettings,
        today: LocalDate
    ): List<Subscription> {
        if (!settings.notificationsEnabled) return emptyList()
        val targetDate = today.plusDays(settings.notificationLead.daysBefore.toLong())
        return subscriptions.filter {
            it.isActive && LocalDate.ofEpochDay(it.nextPaymentEpochDay) == targetDate
        }
    }
}
