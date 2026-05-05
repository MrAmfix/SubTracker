package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BillingRule(
    val type: BillingPeriodType,
    val intervalDays: Int? = null,
    val calendarDay: Int? = null
) {
    fun validate(): Boolean {
        return when (type) {
            BillingPeriodType.WEEKLY -> true
            BillingPeriodType.MONTHLY_FIXED -> (intervalDays ?: 30) > 0
            BillingPeriodType.MONTHLY_CALENDAR_DAY -> calendarDay in 1..31
            BillingPeriodType.YEARLY -> true
            BillingPeriodType.CUSTOM -> (intervalDays ?: 0) > 0
        }
    }

    fun displayName(): String = when (type) {
        BillingPeriodType.WEEKLY -> "Weekly"
        BillingPeriodType.MONTHLY_FIXED -> "Every ${intervalDays ?: 30} days"
        BillingPeriodType.MONTHLY_CALENDAR_DAY -> "Monthly on day ${calendarDay ?: 1}"
        BillingPeriodType.YEARLY -> "Yearly"
        BillingPeriodType.CUSTOM -> "Every ${intervalDays ?: 1} days"
    }
}
