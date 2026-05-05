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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubscriptionListItemUi(
    val subscription: Subscription,
    val priceText: String,
    val convertedPriceText: String?,
    val nextPaymentText: String
)

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
                    nextPaymentText = formatDate(subscription.nextPaymentEpochDay)
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
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            container.subscriptionRepository.delete(id)
            container.notificationScheduler.cancel(id)
            rescheduleNotifications()
        }
    }

    private suspend fun rescheduleNotifications() {
        container.notificationScheduler.rescheduleAll(
            container.subscriptionRepository.getAll(),
            container.settingsRepository.settings.first()
        )
    }
}
