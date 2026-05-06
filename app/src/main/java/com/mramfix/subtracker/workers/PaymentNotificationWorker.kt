package com.mramfix.subtracker.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mramfix.subtracker.SubTrackerApplication
import com.mramfix.subtracker.domain.usecase.CurrencyConverter
import com.mramfix.subtracker.domain.usecase.NextPaymentCalculator
import com.mramfix.subtracker.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

class PaymentNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val subscriptionId = inputData.getLong(KEY_SUBSCRIPTION_ID, -1L)
        if (subscriptionId <= 0) return Result.failure()
        val container = (applicationContext as SubTrackerApplication).container
        val subscription = container.subscriptionRepository.getAll().firstOrNull { it.id == subscriptionId }
            ?: return Result.success()
        if (!subscription.isActive) return Result.success()

        val settings = container.settingsRepository.settings.first()
        val rates = container.currencyRepository.getRates()
        val storedPaymentDate = LocalDate.ofEpochDay(subscription.nextPaymentEpochDay)
        val paymentDate = NextPaymentCalculator.upcomingFrom(
            seedDate = storedPaymentDate,
            today = LocalDate.now(),
            rule = subscription.billingRule
        )
        if (paymentDate != storedPaymentDate) {
            container.subscriptionRepository.upsert(
                subscription.copy(
                    nextPaymentEpochDay = paymentDate.toEpochDay(),
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
        val originalPrice = formatMoney(subscription.cost, subscription.currency.name)
        val converted = CurrencyConverter.convert(
            amount = subscription.cost,
            from = subscription.currency,
            to = settings.baseCurrency,
            rates = rates
        )?.takeIf { settings.baseCurrency != subscription.currency }
            ?.let { " (~${formatMoney(it, settings.baseCurrency.name)})" }
            .orEmpty()
        val text = "$originalPrice$converted · ${paymentDate.format(DisplayDateFormatter)}"
        NotificationHelper.showPaymentNotification(
            applicationContext,
            subscription.id,
            subscription.name,
            text
        )
        container.notificationScheduler.rescheduleAll(container.subscriptionRepository.getAll(), settings)
        return Result.success()
    }

    private fun formatMoney(value: Double, currencyCode: String): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(currencyCode)
            maximumFractionDigits = if (currencyCode == "JPY") 0 else 2
        }.format(value)
    }

    companion object {
        const val KEY_SUBSCRIPTION_ID = "subscription_id"
        private val DisplayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
