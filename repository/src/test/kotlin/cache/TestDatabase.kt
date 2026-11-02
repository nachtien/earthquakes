package com.achtien.repository.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/** A fresh, empty database in memory, built like the app's. */
internal fun inMemoryDatabase(): EarthquakeDatabase =
    EarthquakeDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { EarthquakeDatabase.Schema.create(it) })
