package com.achtien.repository.cache

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.EarthquakeProperties
import com.achtien.networking.PointGeometry
import kotlin.time.Instant

/** Epoch milliseconds, as USGS sends them. */
internal object InstantAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant) = value.toEpochMilliseconds()
}

/** The database with its column adapters, built the same way by the app and the tests. */
fun EarthquakeDatabase(driver: SqlDriver) = EarthquakeDatabase(driver, Earthquake.Adapter(InstantAdapter, InstantAdapter))

internal fun EarthquakeFeature.toRow(fetchedAt: Instant) =
    Earthquake(
        id = id,
        startDate = properties.time,
        location = properties.place,
        latitude = geometry.latitude,
        longitude = geometry.longitude,
        depthKm = geometry.depthKm,
        magnitude = properties.mag,
        magnitudeType = properties.magType,
        fetchedAt = fetchedAt,
    )

internal fun Earthquake.toFeature() =
    EarthquakeFeature(
        id = id,
        properties = EarthquakeProperties(mag = magnitude, magType = magnitudeType, place = location, time = startDate),
        geometry = PointGeometry(listOf(longitude, latitude, depthKm)),
    )
