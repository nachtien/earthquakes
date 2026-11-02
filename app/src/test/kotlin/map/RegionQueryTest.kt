package com.achtien.codingtemplate.map

import com.achtien.networking.singleFeature
import com.achtien.repository.EarthquakeResults
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class RegionQueryTest {
    private val now = Instant.parse("2026-10-30T12:34:56Z")

    @Test
    fun `a normal box maps one to one`() {
        val query = GeoBounds(south = 36.5, west = -123.5, north = 38.5, east = -121.0).toEarthquakeQuery(now)
        assertEquals(36.5, query.minLatitude, 0.0)
        assertEquals(38.5, query.maxLatitude, 0.0)
        assertEquals(-123.5, query.minLongitude, 0.0)
        assertEquals(-121.0, query.maxLongitude, 0.0)
        assertEquals(REGION_LIMIT, query.limit)
    }

    @Test
    fun `the window is the past 30 days`() {
        val query = GeoBounds(0.0, 0.0, 1.0, 1.0).toEarthquakeQuery(now)
        assertEquals(Instant.parse("2026-09-30T12:34:56Z"), query.startTime)
    }

    @Test
    fun `a box crossing the antimeridian extends past 180`() {
        val query = GeoBounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0).toEarthquakeQuery(now)
        assertEquals(170.0, query.minLongitude, 0.0)
        assertEquals(190.0, query.maxLongitude, 0.0)
    }

    @Test
    fun `zooming out raises the magnitude floor`() {
        assertEquals(4.5, GeoBounds(-20.0, 0.0, 20.0, 1.0).minMagnitude(), 0.0)
        assertEquals(2.5, GeoBounds(0.0, 0.0, 10.0, 1.0).minMagnitude(), 0.0)
        assertEquals(1.0, GeoBounds(0.0, 0.0, 3.0, 1.0).minMagnitude(), 0.0)
    }

    @Test
    fun `the headline gives the count, window and magnitude floor`() {
        val query = GeoBounds(0.0, 0.0, 3.0, 1.0).toEarthquakeQuery(now)
        assertEquals(
            "2 earthquakes · past 30 days · M1.0+",
            EarthquakeResults(query, List(2) { singleFeature }, now).headline(),
        )
    }
}
