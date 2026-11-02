package com.achtien.repository

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.achtien.networking.NetworkingModule
import com.achtien.repository.cache.EarthquakeDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Module(includes = [NetworkingModule::class])
@Configuration
@ComponentScan
class RepositoryModule {
    @Single
    fun clock(): Clock = Clock.System

    @Single
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    fun earthquakeDatabase(context: Context): EarthquakeDatabase =
        EarthquakeDatabase(AndroidSqliteDriver(EarthquakeDatabase.Schema, context, "earthquakes.db"))
}
