package com.mramfix.subtracker.data.local

import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.model.SubscriptionStatus

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    iconUri = iconUri,
    description = description,
    cost = cost,
    currency = CurrencyCode.valueOf(currency),
    status = SubscriptionStatus.valueOf(status),
    nextPaymentEpochDay = nextPaymentEpochDay,
    billingRule = BillingRule(
        type = BillingPeriodType.valueOf(billingType),
        intervalDays = intervalDays,
        calendarDay = calendarDay
    ),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id,
    name,
    iconUri,
    description,
    cost,
    currency.name,
    status.name,
    nextPaymentEpochDay,
    billingRule.type.name,
    billingRule.intervalDays,
    billingRule.calendarDay,
    createdAtEpochMillis,
    updatedAtEpochMillis
)

fun ExchangeRateEntity.toDomain(): ExchangeRate = ExchangeRate(
    currency = CurrencyCode.valueOf(currency),
    rubPerUnit = rubPerUnit,
    nominal = nominal,
    fetchedAtEpochMillis = fetchedAtEpochMillis
)

fun ExchangeRate.toEntity(): ExchangeRateEntity = ExchangeRateEntity(
    currency.name,
    rubPerUnit,
    nominal,
    fetchedAtEpochMillis
)
