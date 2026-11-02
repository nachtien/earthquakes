package com.achtien.networking

import kotlin.time.Instant

data class EarthquakeQuery(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val startTime: Instant,
    val minMagnitude: Double,
    val limit: Int,
)
