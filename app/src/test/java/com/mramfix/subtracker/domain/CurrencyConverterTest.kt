package com.mramfix.subtracker.domain

import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate
import com.mramfix.subtracker.domain.usecase.CurrencyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyConverterTest {
    @Test
    fun convertsUsdToEurThroughRub() {
        val rates = mapOf(
            CurrencyCode.USD to ExchangeRate(CurrencyCode.USD, rubPerUnit = 90.0, fetchedAtEpochMillis = 1),
            CurrencyCode.EUR to ExchangeRate(CurrencyCode.EUR, rubPerUnit = 100.0, fetchedAtEpochMillis = 1)
        )

        val result = CurrencyConverter.convert(10.0, CurrencyCode.USD, CurrencyCode.EUR, rates)

        assertEquals(9.0, result!!, 0.0001)
    }

    @Test
    fun returnsNullWhenRateIsMissing() {
        val result = CurrencyConverter.convert(10.0, CurrencyCode.USD, CurrencyCode.CNY, emptyMap())

        assertNull(result)
    }
}
