package com.mramfix.subtracker.presentation

import com.mramfix.subtracker.domain.model.CurrencyCode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val DisplayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun formatMoney(value: Double, currencyCode: CurrencyCode): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = Currency.getInstance(currencyCode.name)
        maximumFractionDigits = if (currencyCode == CurrencyCode.JPY) 0 else 2
    }.format(value)
}

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DisplayDateFormatter)

fun formatDate(date: LocalDate): String = date.format(DisplayDateFormatter)

fun parseDisplayDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value, DisplayDateFormatter) }
        .recoverCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }
        .getOrNull()
}
