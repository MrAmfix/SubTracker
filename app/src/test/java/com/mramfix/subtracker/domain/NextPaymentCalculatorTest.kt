package com.mramfix.subtracker.domain

import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.BillingRule
import com.mramfix.subtracker.domain.usecase.NextPaymentCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NextPaymentCalculatorTest {
    @Test
    fun monthlyFixedKeepsThirtyDayDuration() {
        val next = NextPaymentCalculator.nextAfter(
            LocalDate.of(2026, 1, 31),
            BillingRule(BillingPeriodType.MONTHLY_FIXED, intervalDays = 30)
        )

        assertEquals(LocalDate.of(2026, 3, 2), next)
    }

    @Test
    fun monthlyCalendarDayMovesThirtyToLastFebruaryDay() {
        val next = NextPaymentCalculator.nextAfter(
            LocalDate.of(2026, 1, 30),
            BillingRule(BillingPeriodType.MONTHLY_CALENDAR_DAY, calendarDay = 30)
        )

        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun monthlyCalendarDayHandlesLeapFebruary() {
        val next = NextPaymentCalculator.nextAfter(
            LocalDate.of(2024, 1, 30),
            BillingRule(BillingPeriodType.MONTHLY_CALENDAR_DAY, calendarDay = 30)
        )

        assertEquals(LocalDate.of(2024, 2, 29), next)
    }

    @Test
    fun monthlyCalendarDayReturnsOriginalDayAfterShortMonth() {
        val march = NextPaymentCalculator.nextAfter(
            LocalDate.of(2024, 2, 29),
            BillingRule(BillingPeriodType.MONTHLY_CALENDAR_DAY, calendarDay = 30)
        )

        assertEquals(LocalDate.of(2024, 3, 30), march)
    }
}
