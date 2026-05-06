package com.mramfix.subtracker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mramfix.subtracker.SubTrackerApplication
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate
import com.mramfix.subtracker.domain.model.SortMode
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.usecase.CurrencyConverter
import com.mramfix.subtracker.domain.usecase.NextPaymentCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SubscriptionListItemUi(
    val subscription: Subscription,
    val priceText: String,
    val convertedPriceText: String?,
    val nextPaymentText: String,
    val paymentCountdown: PaymentCountdownUi?
)

data class PaymentCountdownUi(
    val text: String,
    val urgency: PaymentCountdownUrgency
)

enum class PaymentCountdownUrgency {
    RED,
    ORANGE,
    YELLOW
}

data class HomeUiState(
    val items: List<SubscriptionListItemUi> = emptyList(),
    val statsSummary: HomeStatsSummaryUi = HomeStatsSummaryUi(),
    val settings: AppSettings = AppSettings(),
    val sortMode: SortMode = SortMode.NEXT_PAYMENT,
    val refreshingRates: Boolean = false,
    val message: String? = null
)

private data class HomeData(
    val subscriptions: List<Subscription>,
    val settings: AppSettings,
    val rates: Map<CurrencyCode, ExchangeRate>,
    val sortMode: SortMode
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SubTrackerApplication).container
    private val sortMode = MutableStateFlow(SortMode.NEXT_PAYMENT)
    private val refreshingRates = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val homeData = combine(
        container.subscriptionRepository.observeAll(),
        container.settingsRepository.settings,
        container.currencyRepository.rates,
        sortMode
    ) { subscriptions, settings, rates, sort ->
        HomeData(subscriptions, settings, rates, sort)
    }

    val uiState = combine(homeData, refreshingRates, message) { data, refreshing, currentMessage ->
        val sorted = when (data.sortMode) {
            SortMode.NEXT_PAYMENT -> data.subscriptions.sortedBy { it.nextPaymentEpochDay }
            SortMode.PAYMENT_DAY -> data.subscriptions.sortedWith(
                compareBy<Subscription> { LocalDate.ofEpochDay(it.nextPaymentEpochDay).dayOfMonth }
                    .thenBy { it.name.lowercase() }
            )
            SortMode.COST -> data.subscriptions.sortedByDescending { subscription ->
                CurrencyConverter.convert(subscription.cost, subscription.currency, data.settings.baseCurrency, data.rates)
                    ?: subscription.cost
            }
            SortMode.NAME -> data.subscriptions.sortedBy { it.name.lowercase() }
        }
        HomeUiState(
            items = sorted.map { subscription ->
                val converted = CurrencyConverter.convert(
                    amount = subscription.cost,
                    from = subscription.currency,
                    to = data.settings.baseCurrency,
                    rates = data.rates
                )?.takeIf { subscription.currency != data.settings.baseCurrency }
                SubscriptionListItemUi(
                    subscription = subscription,
                    priceText = formatMoney(subscription.cost, subscription.currency),
                    convertedPriceText = converted?.let { "≈ ${formatMoney(it, data.settings.baseCurrency)}" },
                    nextPaymentText = formatDate(subscription.nextPaymentEpochDay),
                    paymentCountdown = paymentCountdown(
                        paymentDate = LocalDate.ofEpochDay(subscription.nextPaymentEpochDay),
                        today = LocalDate.now()
                    )
                )
            },
            statsSummary = StatisticsCalculator.homeSummary(data.subscriptions, data.settings, data.rates),
            settings = data.settings,
            sortMode = data.sortMode,
            refreshingRates = refreshing,
            message = currentMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            container.currencyRepository.ensureFreshRates()
        }
        viewModelScope.launch {
            container.subscriptionRepository.observeAll().collect { subscriptions ->
                updateOverduePayments(subscriptions)
            }
        }
        viewModelScope.launch {
            container.autoSyncManager.syncErrors.collect { error ->
                message.value = error
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        sortMode.value = mode
    }

    fun refreshRates() {
        viewModelScope.launch {
            refreshingRates.value = true
            val result = container.currencyRepository.refreshRates()
            refreshingRates.value = false
            message.value = result.fold(
                onSuccess = { "Курсы валют обновлены" },
                onFailure = { "Не удалось обновить курсы. Старый кэш будет использован, если он есть" }
            )
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            container.subscriptionRepository.setActive(id, active)
            rescheduleNotifications()
            autoSyncAfterChange()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            container.subscriptionRepository.delete(id)
            container.notificationScheduler.cancel(id)
            rescheduleNotifications()
            autoSyncAfterChange()
        }
    }

    private suspend fun autoSyncAfterChange() {
        container.autoSyncManager.requestSyncIfEnabled()
    }

    private suspend fun rescheduleNotifications() {
        container.notificationScheduler.rescheduleAll(
            container.subscriptionRepository.getAll(),
            container.settingsRepository.settings.first()
        )
    }

    private suspend fun updateOverduePayments(subscriptions: List<Subscription>) {
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val updated = subscriptions.mapNotNull { subscription ->
            if (!subscription.billingRule.validate()) return@mapNotNull null
            val currentDate = LocalDate.ofEpochDay(subscription.nextPaymentEpochDay)
            val upcomingDate = NextPaymentCalculator.upcomingFrom(currentDate, today, subscription.billingRule)
            if (upcomingDate == currentDate) {
                null
            } else {
                subscription.copy(
                    nextPaymentEpochDay = upcomingDate.toEpochDay(),
                    updatedAtEpochMillis = now
                )
            }
        }
        if (updated.isEmpty()) return
        updated.forEach { container.subscriptionRepository.upsert(it) }
        rescheduleNotifications()
        autoSyncAfterChange()
    }
}

private fun paymentCountdown(paymentDate: LocalDate, today: LocalDate): PaymentCountdownUi? {
    val daysUntilPayment = ChronoUnit.DAYS.between(today, paymentDate).toInt()
    return when {
        daysUntilPayment == 0 -> PaymentCountdownUi("Сегодня", PaymentCountdownUrgency.RED)
        daysUntilPayment == 1 -> PaymentCountdownUi("Завтра", PaymentCountdownUrgency.RED)
        daysUntilPayment in 2..6 -> PaymentCountdownUi("Через $daysUntilPayment ${dayWord(daysUntilPayment)}", PaymentCountdownUrgency.ORANGE)
        daysUntilPayment in 7..34 -> {
            val weeks = daysUntilPayment / 7
            PaymentCountdownUi("Через $weeks ${weekWord(weeks)}", PaymentCountdownUrgency.YELLOW)
        }
        else -> null
    }
}

fun isPaymentTodayOrTomorrow(paymentDate: LocalDate, today: LocalDate): Boolean {
    return ChronoUnit.DAYS.between(today, paymentDate) in 0L..1L
}

private fun dayWord(value: Int): String {
    return when {
        value % 100 in 11..14 -> "дней"
        value % 10 == 1 -> "день"
        value % 10 in 2..4 -> "дня"
        else -> "дней"
    }
}

private fun weekWord(value: Int): String {
    return when {
        value % 100 in 11..14 -> "недель"
        value % 10 == 1 -> "неделю"
        value % 10 in 2..4 -> "недели"
        else -> "недель"
    }
}
