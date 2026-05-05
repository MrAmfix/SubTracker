package com.mramfix.subtracker.data.repository

import com.mramfix.subtracker.data.local.ExchangeRateDao
import com.mramfix.subtracker.data.local.toDomain
import com.mramfix.subtracker.data.local.toEntity
import com.mramfix.subtracker.data.remote.CbrApi
import com.mramfix.subtracker.data.remote.CbrDailyResponse
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext

class CurrencyRepository(
    private val dao: ExchangeRateDao,
    private val api: CbrApi,
    private val json: Json
) {
    val rates: Flow<Map<CurrencyCode, ExchangeRate>> = dao.observeAll().map { rows ->
        rows.map { it.toDomain() }.associateBy { it.currency }
    }

    suspend fun getRates(): Map<CurrencyCode, ExchangeRate> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }.associateBy { it.currency }
        }
    }

    suspend fun ensureFreshRates(): Result<Unit> {
        val cached = withContext(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
        val newestFetch = cached.maxOfOrNull { it.fetchedAtEpochMillis } ?: 0L
        val hasSupportedRates = CurrencyCode.entries.all { code ->
            code == CurrencyCode.RUB || cached.any { it.currency == code }
        }
        val fresh = System.currentTimeMillis() - newestFetch < TimeUnit.HOURS.toMillis(24)
        return if (fresh && hasSupportedRates) Result.success(Unit) else refreshRates()
    }

    suspend fun refreshRates(): Result<Unit> = runCatching {
        val response = api.dailyRates()
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val body = response.body()?.string() ?: error("Empty response body")
        val parsed = json.decodeFromString<CbrDailyResponse>(body)
        val now = System.currentTimeMillis()
        val rates = buildList {
            add(ExchangeRate(CurrencyCode.RUB, rubPerUnit = 1.0, nominal = 1, fetchedAtEpochMillis = now))
            CurrencyCode.entries
                .filterNot { it == CurrencyCode.RUB }
                .forEach { code ->
                    val cbrRate = parsed.valute[code.name] ?: error("Missing ${code.name} in CBR response")
                    add(
                        ExchangeRate(
                            currency = code,
                            rubPerUnit = cbrRate.value / cbrRate.nominal,
                            nominal = cbrRate.nominal,
                            fetchedAtEpochMillis = now
                        )
                    )
                }
        }
        withContext(Dispatchers.IO) {
            dao.upsertAll(rates.map { it.toEntity() })
        }
    }
}
