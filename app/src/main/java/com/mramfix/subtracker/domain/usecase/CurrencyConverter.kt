package com.mramfix.subtracker.domain.usecase

import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate

object CurrencyConverter {
    fun convert(
        amount: Double,
        from: CurrencyCode,
        to: CurrencyCode,
        rates: Map<CurrencyCode, ExchangeRate>
    ): Double? {
        if (from == to) return amount
        val rubAmount = when (from) {
            CurrencyCode.RUB -> amount
            else -> amount * (rates[from]?.rubPerUnit ?: return null)
        }
        return when (to) {
            CurrencyCode.RUB -> rubAmount
            else -> rubAmount / (rates[to]?.rubPerUnit ?: return null)
        }
    }
}
