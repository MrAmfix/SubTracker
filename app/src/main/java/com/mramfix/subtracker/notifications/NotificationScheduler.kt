package com.mramfix.subtracker.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.usecase.NextPaymentCalculator
import com.mramfix.subtracker.workers.PaymentNotificationWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun rescheduleAll(subscriptions: List<Subscription>, settings: AppSettings) {
        workManager.cancelAllWorkByTag(TAG)
        if (!settings.notificationsEnabled) return
        subscriptions.filter { it.isActive }.forEach { schedule(it, settings) }
    }

    fun cancel(subscriptionId: Long) {
        workManager.cancelUniqueWork(workName(subscriptionId))
    }

    private fun schedule(subscription: Subscription, settings: AppSettings) {
        val now = LocalDateTime.now()
        var paymentDate = LocalDate.ofEpochDay(subscription.nextPaymentEpochDay)
        var notifyAt = notificationDateTime(paymentDate, settings)
        var guard = 0
        while (!notifyAt.isAfter(now) && guard < 1000) {
            paymentDate = NextPaymentCalculator.nextAfter(paymentDate, subscription.billingRule)
            notifyAt = notificationDateTime(paymentDate, settings)
            guard++
        }
        val delayMillis = Duration.between(
            now.atZone(ZoneId.systemDefault()).toInstant(),
            notifyAt.atZone(ZoneId.systemDefault()).toInstant()
        ).toMillis().coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<PaymentNotificationWorker>()
            .setInputData(workDataOf(PaymentNotificationWorker.KEY_SUBSCRIPTION_ID to subscription.id))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(workName(subscription.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun notificationDateTime(paymentDate: LocalDate, settings: AppSettings): LocalDateTime {
        return paymentDate
            .minusDays(settings.notificationLead.daysBefore.toLong())
            .atTime(LocalTime.of(10, 0))
    }

    private fun workName(subscriptionId: Long) = "subscription_notification_$subscriptionId"

    companion object {
        const val TAG = "subscription_notifications"
    }
}
