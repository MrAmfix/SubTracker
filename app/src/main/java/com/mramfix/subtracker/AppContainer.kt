package com.mramfix.subtracker

import android.content.Context
import com.mramfix.subtracker.data.local.AppDatabase
import com.mramfix.subtracker.data.remote.CbrApi
import com.mramfix.subtracker.data.repository.CurrencyRepository
import com.mramfix.subtracker.data.repository.SettingsRepository
import com.mramfix.subtracker.data.repository.SubscriptionRepository
import com.mramfix.subtracker.data.settings.SettingsDataStore
import com.mramfix.subtracker.importexport.BackupJsonCodec
import com.mramfix.subtracker.importexport.ImportExportRepository
import com.mramfix.subtracker.notifications.NotificationScheduler
import kotlinx.serialization.json.Json
import retrofit2.Retrofit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)

    private val cbrApi: CbrApi = Retrofit.Builder()
        .baseUrl("https://www.cbr-xml-daily.ru/")
        .build()
        .create(CbrApi::class.java)

    val json: Json = BackupJsonCodec.json
    val subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
    val settingsRepository = SettingsRepository(SettingsDataStore(appContext))
    val currencyRepository = CurrencyRepository(database.exchangeRateDao(), cbrApi, json)
    val importExportRepository = ImportExportRepository(subscriptionRepository, settingsRepository)
    val notificationScheduler = NotificationScheduler(appContext)
}
