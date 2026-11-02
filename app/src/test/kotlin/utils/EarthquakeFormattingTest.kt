package com.achtien.codingtemplate.utils

import com.achtien.networking.PointGeometry
import com.achtien.repository.location.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class EarthquakeFormattingTest {
    @Test
    fun `distance is in miles with one decimal`() {
        assertEquals("62.1 miles", formatDistance(100.0))
        assertEquals("0.0 miles", formatDistance(0.0))
    }

    @Test
    fun `event time is a US date and 12-hour time in the given zone`() {
        // 2026-10-31T02:23:51Z is 7:23:51 PM on Oct 30 in Los Angeles (PDT).
        assertEquals("10/30, 7:23:51 PM", formatEventTime(Instant.parse("2026-10-31T02:23:51Z"), TimeZone.of("America/Los_Angeles")))
    }

    @Test
    fun `place title drops the distance prefix`() {
        assertEquals("Cloverdale, CA", placeTitle("8 km ESE of Cloverdale, CA"))
        assertEquals("Alaska Peninsula", placeTitle("Alaska Peninsula"))
        assertEquals("Unknown location", placeTitle(null))
    }

    @Test
    fun `distance text gives the distance or why it is missing`() {
        val sanFrancisco = PointGeometry(listOf(-122.42, 37.77, 10.0))
        assertEquals("0.0 miles", UserLocation.Known(37.77, -122.42).distanceText(sanFrancisco))
        assertEquals("Locating…", UserLocation.Unknown.distanceText(sanFrancisco))
        assertEquals("Unavailable", UserLocation.Unavailable.distanceText(sanFrancisco))
        assertEquals("Location off", UserLocation.PermissionDenied.distanceText(sanFrancisco))
        assertEquals("Location off", UserLocation.ServicesOff.distanceText(sanFrancisco))
    }
}
