package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val id: Long = 0,
    val name: String,
    val iconUri: String? = null,
    val description: String? = null,
    val cost: Double,
    val currency: CurrencyCode,
    val status: SubscriptionStatus,
    val nextPaymentEpochDay: Long,
    val billingRule: BillingRule,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    val isActive: Boolean
        get() = status == SubscriptionStatus.ACTIVE
}
