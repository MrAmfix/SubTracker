package com.mramfix.subtracker.presentation

import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.usecase.CurrencyConverter
import java.time.LocalDate

data class MoneyTotalUi(
    val value: Double?,
    val text: String,
    val hasMissingRates: Boolean
)

data class HomeStatsSummaryUi(
    val activeTotalText: String = "—",
    val allTotalText: String = "—",
    val baseCurrency: CurrencyCode = CurrencyCode.RUB,
    val hasMissingRates: Boolean = false
)

data class CurrencyRateUi(
    val currency: CurrencyCode,
    val text: String
)

data class PaymentPeriodUi(
    val title: String,
    val totalText: String,
    val count: Int,
    val hasMissingRates: Boolean
)

data class DetailedStatsUi(
    val baseCurrency: CurrencyCode = CurrencyCode.RUB,
    val activeTotalText: String = "—",
    val allTotalText: String = "—",
    val activeCount: Int = 0,
    val allCount: Int = 0,
    val rates: List<CurrencyRateUi> = emptyList(),
    val salaryDayText: String = "5",
    val advanceDayText: String = "20",
    val salaryPeriod: PaymentPeriodUi = PaymentPeriodUi("С 5 по 19", "—", 0, false),
    val advancePeriod: PaymentPeriodUi = PaymentPeriodUi("С 20 по 4", "—", 0, false),
    val inputError: String? = null
)

object StatisticsCalculator {
    fun homeSummary(
        subscriptions: List<Subscription>,
        settings: AppSettings,
        rates: Map<CurrencyCode, ExchangeRate>
    ): HomeStatsSummaryUi {
        val activeTotal = total(subscriptions.filter { it.isActive }, settings.baseCurrency, rates)
        val allTotal = total(subscriptions, settings.baseCurrency, rates)
        return HomeStatsSummaryUi(
            activeTotalText = activeTotal.text,
            allTotalText = allTotal.text,
            baseCurrency = settings.baseCurrency,
            hasMissingRates = activeTotal.hasMissingRates || allTotal.hasMissingRates
        )
    }

    fun detailed(
        subscriptions: List<Subscription>,
        settings: AppSettings,
        rates: Map<CurrencyCode, ExchangeRate>
    ): DetailedStatsUi {
        val active = subscriptions.filter { it.isActive }
        val activeTotal = total(active, settings.baseCurrency, rates)
        val allTotal = total(subscriptions, settings.baseCurrency, rates)
        val salaryDay = settings.salaryDay.coerceIn(1, 31)
        val advanceDay = settings.advanceDay.coerceIn(1, 31)
        val salaryEnd = previousDay(advanceDay)
        val advanceEnd = previousDay(salaryDay)
        val salarySubscriptions = active.filter {
            dayInRange(LocalDate.ofEpochDay(it.nextPaymentEpochDay).dayOfMonth, salaryDay, salaryEnd)
        }
        val advanceSubscriptions = active.filter {
            dayInRange(LocalDate.ofEpochDay(it.nextPaymentEpochDay).dayOfMonth, advanceDay, advanceEnd)
        }
        val salaryTotal = total(salarySubscriptions, settings.baseCurrency, rates)
        val advanceTotal = total(advanceSubscriptions, settings.baseCurrency, rates)

        return DetailedStatsUi(
            baseCurrency = settings.baseCurrency,
            activeTotalText = activeTotal.text,
            allTotalText = allTotal.text,
            activeCount = active.size,
            allCount = subscriptions.size,
            rates = CurrencyCode.entries.filterNot { it == CurrencyCode.RUB }.map { code ->
                CurrencyRateUi(
                    currency = code,
                    text = rates[code]?.let { "1 ${code.name} = ${"%.4f".format(it.rubPerUnit)} RUB" }
                        ?: "1 ${code.name} = —"
                )
            },
            salaryDayText = salaryDay.toString(),
            advanceDayText = advanceDay.toString(),
            salaryPeriod = PaymentPeriodUi(
                title = "С $salaryDay по $salaryEnd",
                totalText = salaryTotal.text,
                count = salarySubscriptions.size,
                hasMissingRates = salaryTotal.hasMissingRates
            ),
            advancePeriod = PaymentPeriodUi(
                title = "С $advanceDay по $advanceEnd",
                totalText = advanceTotal.text,
                count = advanceSubscriptions.size,
                hasMissingRates = advanceTotal.hasMissingRates
            ),
            inputError = if (salaryDay == advanceDay) "Дни зарплаты и аванса не должны совпадать" else null
        )
    }

    private fun total(
        subscriptions: List<Subscription>,
        baseCurrency: CurrencyCode,
        rates: Map<CurrencyCode, ExchangeRate>
    ): MoneyTotalUi {
        var missingRates = false
        var sum = 0.0
        subscriptions.forEach { subscription ->
            val converted = CurrencyConverter.convert(
                amount = subscription.cost,
                from = subscription.currency,
                to = baseCurrency,
                rates = rates
            )
            if (converted == null) {
                missingRates = true
            } else {
                sum += converted
            }
        }
        return MoneyTotalUi(
            value = if (missingRates) null else sum,
            text = if (missingRates) "—" else formatMoney(sum, baseCurrency),
            hasMissingRates = missingRates
        )
    }

    private fun previousDay(day: Int): Int = if (day <= 1) 31 else day - 1

    private fun dayInRange(day: Int, start: Int, end: Int): Boolean {
        return if (start <= end) {
            day in start..end
        } else {
            day >= start || day <= end
        }
    }
}
