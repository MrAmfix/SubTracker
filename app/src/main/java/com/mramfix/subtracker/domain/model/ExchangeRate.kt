package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRate(
    val currency: CurrencyCode,
    val rubPerUnit: Double,
    val nominal: Int = 1,
    val fetchedAtEpochMillis: Long
)
