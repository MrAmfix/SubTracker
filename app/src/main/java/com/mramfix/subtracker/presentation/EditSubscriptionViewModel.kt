package com.mramfix.subtracker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mramfix.subtracker.SubTrackerApplication
import com.mramfix.subtracker.cloudbackup.AutoSyncResult
import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.model.SubscriptionStatus
import com.mramfix.subtracker.domain.usecase.NextPaymentCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class SubscriptionFormState(
    val id: Long = 0,
    val name: String = "",
    val iconUri: String = "",
    val description: String = "",
    val cost: String = "",
    val currency: CurrencyCode = CurrencyCode.RUB,
    val active: Boolean = true,
    val nextPaymentDate: String = formatDate(LocalDate.now()),
    val billingType: BillingPeriodType = BillingPeriodType.MONTHLY_CALENDAR_DAY,
    val intervalDays: String = "30",
    val calendarDay: String = LocalDate.now().dayOfMonth.toString(),
    val error: String? = null,
    val loaded: Boolean = false
)

class EditSubscriptionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val container = (application as SubTrackerApplication).container
    private val subscriptionId: Long = savedStateHandle["subscriptionId"] ?: 0L
    private val _state = MutableStateFlow(SubscriptionFormState(id = subscriptionId))
    val state: StateFlow<SubscriptionFormState> = _state.asStateFlow()
    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved

    init {
        viewModelScope.launch {
            if (subscriptionId > 0) {
                val subscription = container.subscriptionRepository.observeById(subscriptionId).first()
                if (subscription != null) fill(subscription) else _state.update { it.copy(loaded = true) }
            } else {
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    fun updateName(value: String) = update { copy(name = value, error = null) }
    fun updateIconUri(value: String) = update { copy(iconUri = value, error = null) }
    fun updateDescription(value: String) = update { copy(description = value, error = null) }
    fun updateCost(value: String) = update { copy(cost = value, error = null) }
    fun updateCurrency(value: CurrencyCode) = update { copy(currency = value, error = null) }
    fun updateActive(value: Boolean) = update { copy(active = value, error = null) }
    fun updateNextPaymentDate(value: String) = update {
        val parsed = parseDisplayDate(value)
        copy(
            nextPaymentDate = value,
            calendarDay = if (parsed != null && billingType == BillingPeriodType.MONTHLY_CALENDAR_DAY) {
                parsed.dayOfMonth.toString()
            } else {
                calendarDay
            },
            error = null
        )
    }
    fun updateNextPaymentDate(value: LocalDate) = updateNextPaymentDate(formatDate(value))
    fun updateBillingType(value: BillingPeriodType) = update {
        val parsed = parseDisplayDate(nextPaymentDate)
        copy(
            billingType = value,
            calendarDay = if (value == BillingPeriodType.MONTHLY_CALENDAR_DAY && parsed != null) {
                parsed.dayOfMonth.toString()
            } else {
                calendarDay
            },
            error = null
        )
    }
    fun updateIntervalDays(value: String) = update { copy(intervalDays = value, error = null) }
    fun updateCalendarDay(value: String) = update { copy(calendarDay = value, error = null) }

    fun save() {
        viewModelScope.launch {
            val form = state.value
            val cost = form.cost.replace(',', '.').toDoubleOrNull()
            val date = parseDisplayDate(form.nextPaymentDate)
            val rule = buildRule(form)
            val error = when {
                form.name.isBlank() -> "Название не может быть пустым"
                cost == null || cost <= 0.0 -> "Стоимость должна быть больше 0"
                date == null -> "Дата оплаты должна быть в формате DD.MM.YYYY"
                rule == null || !rule.validate() -> "Правило оплаты некорректно"
                !dateMatchesRule(date, rule) ->
                    "Дата оплаты должна соответствовать выбранному дню месяца"
                else -> null
            }
            if (error != null) {
                _state.update { it.copy(error = error) }
                return@launch
            }
            val now = System.currentTimeMillis()
            val createdAt = if (form.id == 0L) now else {
                container.subscriptionRepository.observeById(form.id).first()?.createdAtEpochMillis ?: now
            }
            val upcomingDate = NextPaymentCalculator.upcomingFrom(date!!, LocalDate.now(), rule!!)
            val savedId = container.subscriptionRepository.upsert(
                Subscription(
                    id = form.id,
                    name = form.name.trim(),
                    iconUri = form.iconUri.trim().ifBlank { null },
                    description = form.description.trim().ifBlank { null },
                    cost = cost!!,
                    currency = form.currency,
                    status = if (form.active) SubscriptionStatus.ACTIVE else SubscriptionStatus.INACTIVE,
                    nextPaymentEpochDay = upcomingDate.toEpochDay(),
                    billingRule = rule,
                    createdAtEpochMillis = createdAt,
                    updatedAtEpochMillis = now
                )
            )
            container.notificationScheduler.rescheduleAll(
                container.subscriptionRepository.getAll(),
                container.settingsRepository.settings.first()
            )
            _state.update { it.copy(id = if (it.id == 0L) savedId else it.id) }
            when (val syncResult = container.autoSyncManager.syncIfEnabled()) {
                AutoSyncResult.Success, null -> Unit
                is AutoSyncResult.Error -> {
                    _state.update {
                        it.copy(error = "Подписка сохранена, но автосинхронизация не выполнена: ${syncResult.message}")
                    }
                    return@launch
                }
            }
            _saved.emit(Unit)
        }
    }

    private fun fill(subscription: Subscription) {
        _state.value = SubscriptionFormState(
            id = subscription.id,
            name = subscription.name,
            iconUri = subscription.iconUri.orEmpty(),
            description = subscription.description.orEmpty(),
            cost = subscription.cost.toString(),
            currency = subscription.currency,
            active = subscription.isActive,
            nextPaymentDate = formatDate(LocalDate.ofEpochDay(subscription.nextPaymentEpochDay)),
            billingType = subscription.billingRule.type,
            intervalDays = (subscription.billingRule.intervalDays ?: 30).toString(),
            calendarDay = (subscription.billingRule.calendarDay ?: LocalDate.ofEpochDay(subscription.nextPaymentEpochDay).dayOfMonth).toString(),
            loaded = true
        )
    }

    private fun buildRule(form: SubscriptionFormState): BillingRule? {
        return when (form.billingType) {
            BillingPeriodType.WEEKLY -> BillingRule(BillingPeriodType.WEEKLY)
            BillingPeriodType.MONTHLY_FIXED -> BillingRule(
                BillingPeriodType.MONTHLY_FIXED,
                intervalDays = form.intervalDays.toIntOrNull() ?: return null
            )
            BillingPeriodType.MONTHLY_CALENDAR_DAY -> BillingRule(
                BillingPeriodType.MONTHLY_CALENDAR_DAY,
                calendarDay = form.calendarDay.toIntOrNull() ?: return null
            )
            BillingPeriodType.YEARLY -> BillingRule(BillingPeriodType.YEARLY)
            BillingPeriodType.CUSTOM -> BillingRule(
                BillingPeriodType.CUSTOM,
                intervalDays = form.intervalDays.toIntOrNull() ?: return null
            )
        }
    }

    private fun dateMatchesRule(date: LocalDate, rule: BillingRule): Boolean {
        if (rule.type != BillingPeriodType.MONTHLY_CALENDAR_DAY) return true
        val calendarDay = rule.calendarDay ?: return false
        val lastDayOfMonth = YearMonth.from(date).lengthOfMonth()
        return date.dayOfMonth == calendarDay.coerceAtMost(lastDayOfMonth)
    }

    private fun update(block: SubscriptionFormState.() -> SubscriptionFormState) {
        _state.update(block)
    }
}
