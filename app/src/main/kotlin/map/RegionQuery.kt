package com.achtien.codingtemplate.map

import com.achtien.networking.EarthquakeQuery
import com.achtien.repository.EarthquakeResults
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

fun LatLngBounds.toGeoBounds() =
    GeoBounds(southwest.latitude, southwest.longitude, northeast.latitude, northeast.longitude)

/** A lat/lng box as the map reports it: [west] > [east] when it crosses the antimeridian. */
data class GeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
)

val REGION_WINDOW: Duration = 30.days
const val REGION_LIMIT = 500

private const val DEGREES_IN_CIRCLE = 360

/**
 * How zoomed out the map is, in degrees of latitude from bottom to top (1° ≈ 69 miles). Longitude
 * makes a poorer measure: it wraps at ±180 and a degree of it shrinks towards the poles.
 */
val GeoBounds.latitudeSpan: Double get() = north - south

/**
 * The smallest magnitude worth asking for. Zoomed out, small quakes would fill the [REGION_LIMIT]
 * and push out the big ones, so the floor rises with the area shown. The tiers match USGS's own
 * feeds (M4.5+, M2.5+, M1.0+).
 */
fun GeoBounds.minMagnitude(): Double =
    MagnitudeTier.entries.first { latitudeSpan >= it.minLatitudeSpanDegrees }.minMagnitude

/**
 * The magnitude floor for each map size, widest first.
 *
 * - [minMagnitude] is an earthquake magnitude. The values are USGS's own tiers, as in its summary
 *   feeds ("M4.5+", "M2.5+", "M1.0+").
 * - [minLatitudeSpanDegrees] is the map's height in degrees of latitude (1° ≈ 69 miles) from which
 *   the tier applies. These are tuned by eye: the floor rises at about continent and state size.
 */
private enum class MagnitudeTier(
    val minLatitudeSpanDegrees: Double,
    val minMagnitude: Double,
) {
    Continent(minLatitudeSpanDegrees = 30.0, minMagnitude = 4.5), // ≈ 2,070 miles tall
    State(minLatitudeSpanDegrees = 8.0, minMagnitude = 2.5), // ≈ 550 miles tall
    City(minLatitudeSpanDegrees = 0.0, minMagnitude = 1.0), // anything smaller
}

fun GeoBounds.toEarthquakeQuery(now: Instant): EarthquakeQuery =
    EarthquakeQuery(
        minLatitude = south,
        maxLatitude = north,
        minLongitude = west,
        // USGS accepts longitudes in [-360, 360] precisely so a box can cross ±180.
        maxLongitude = if (east < west) east + DEGREES_IN_CIRCLE else east,
        startTime = now - REGION_WINDOW,
        minMagnitude = minMagnitude(),
        limit = REGION_LIMIT,
    )

fun EarthquakeResults.headline() =
    "${earthquakes.size} earthquakes · past ${REGION_WINDOW.inWholeDays} days · M${query.minMagnitude}+"
