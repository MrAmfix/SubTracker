package com.mramfix.subtracker.domain.usecase

import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import java.time.LocalDate
import java.time.YearMonth

object NextPaymentCalculator {
    fun nextAfter(paymentDate: LocalDate, rule: BillingRule): LocalDate {
        return when (rule.type) {
            BillingPeriodType.WEEKLY -> paymentDate.plusDays(7)
            BillingPeriodType.MONTHLY_FIXED -> paymentDate.plusDays((rule.intervalDays ?: 30).toLong())
            BillingPeriodType.MONTHLY_CALENDAR_DAY -> {
                val day = requireNotNull(rule.calendarDay) { "calendarDay is required" }
                val nextMonth = YearMonth.from(paymentDate).plusMonths(1)
                nextMonth.atDay(day.coerceAtMost(nextMonth.lengthOfMonth()))
            }
            BillingPeriodType.YEARLY -> paymentDate.plusYears(1)
            BillingPeriodType.CUSTOM -> paymentDate.plusDays(requireNotNull(rule.intervalDays).toLong())
        }
    }

    fun upcomingFrom(seedDate: LocalDate, today: LocalDate, rule: BillingRule): LocalDate {
        require(rule.validate()) { "Invalid billing rule: $rule" }
        var current = seedDate
        var guard = 0
        while (current.isBefore(today) && guard < 1000) {
            current = nextAfter(current, rule)
            guard++
        }
        return current
    }
}
