package com.achtien.codingtemplate

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

internal object KoinApp

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin<KoinApp> {
            androidLogger()
            androidContext(this@App)
        }
    }
}
