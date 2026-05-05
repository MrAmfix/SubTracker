package com.mramfix.subtracker

import android.app.Application
import com.mramfix.subtracker.notifications.NotificationHelper

class SubTrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
    }
}
