package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BillingPeriodType {
    WEEKLY,
    MONTHLY_FIXED,
    MONTHLY_CALENDAR_DAY,
    YEARLY,
    CUSTOM
}
