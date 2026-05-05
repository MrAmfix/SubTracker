package com.mramfix.subtracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CbrDailyResponse(
    @SerialName("Valute") val valute: Map<String, CbrCurrency> = emptyMap()
)

@Serializable
data class CbrCurrency(
    @SerialName("CharCode") val charCode: String,
    @SerialName("Nominal") val nominal: Int,
    @SerialName("Value") val value: Double
)
