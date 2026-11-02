package com.achtien.repository.cache

import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.EarthquakeQuery
import com.achtien.networking.PointGeometry
import com.achtien.networking.singleFeature
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EarthquakeQueriesTest {
    private val start = singleFeature.properties.time
    private val query =
        EarthquakeQuery(
            minLatitude = 30.0,
            maxLatitude = 40.0,
            minLongitude = -125.0,
            maxLongitude = -120.0,
            startTime = start,
            minMagnitude = 0.23,
            limit = 500,
        )
    private val queries = inMemoryDatabase().earthquakeQueries

    private fun quake(
        id: String = "q",
        latitude: Double = 35.0,
        longitude: Double = -122.0,
        mag: Double? = 0.23,
        hoursAfterStart: Int = 0,
    ) = singleFeature.copy(
        id = id,
        properties = singleFeature.properties.copy(mag = mag, time = start + hoursAfterStart.hours),
        geometry = PointGeometry(listOf(longitude, latitude, 1.0)),
    )

    /** Whether [query] finds [this] once it's the only row. */
    private fun EarthquakeFeature.matches(query: EarthquakeQuery = this@EarthquakeQueriesTest.query): Boolean {
        queries.transaction {
            queries.deleteOlderThan(Instant.DISTANT_FUTURE)
            queries.upsert(toRow(start))
        }
        return select(query).isNotEmpty()
    }

    private fun select(query: EarthquakeQuery) =
        queries
            .selectMatching(
                query.minMagnitude,
                query.startTime,
                query.minLatitude,
                query.maxLatitude,
                query.minLongitude,
                query.maxLongitude,
                query.limit.toLong(),
            ).executeAsList()

    @Test
    fun `inside the box, at the floor and at the start time matches`() = assertEquals(true, quake().matches())

    @Test
    fun `outside the box does not match`() {
        assertEquals(false, quake(latitude = 41.0).matches())
        assertEquals(false, quake(longitude = -119.0).matches())
    }

    @Test
    fun `below the floor or without a magnitude does not match`() {
        assertEquals(false, quake(mag = 0.2).matches())
        assertEquals(false, quake(mag = null).matches())
    }

    @Test
    fun `older than the window does not match`() = assertEquals(false, quake().matches(query.copy(startTime = start + 1.seconds)))

    @Test
    fun `a box across the antimeridian matches either side of it`() {
        val pacific = query.copy(minLongitude = 170.0, maxLongitude = 190.0)
        assertEquals(true, quake(longitude = 175.0).matches(pacific))
        assertEquals(true, quake(longitude = -175.0).matches(pacific))
        assertEquals(false, quake(longitude = 160.0).matches(pacific))
        assertEquals(false, quake(longitude = -165.0).matches(pacific))
    }

    @Test
    fun `answers newest first and at most the limit`() {
        (0..2).forEach { queries.upsert(quake("$it", hoursAfterStart = it).toRow(start)) }
        assertEquals(listOf("2", "1"), select(query.copy(limit = 2)).map { it.id })
    }

    @Test
    fun `upsert replaces a row by id`() {
        queries.upsert(quake(mag = 1.0).toRow(start))
        queries.upsert(quake(mag = 5.0).toRow(start))
        assertEquals(listOf(5.0), select(query).map { it.magnitude })
    }

    @Test
    fun `deleteOlderThan removes only older rows`() {
        queries.upsert(quake("old").toRow(start))
        queries.upsert(quake("new", hoursAfterStart = 2).toRow(start))
        queries.deleteOlderThan(start + 1.hours)
        assertEquals(listOf("new"), select(query).map { it.id })
    }

    @Test
    fun `a feature survives the round trip through a row`() = assertEquals(singleFeature, singleFeature.toRow(start).toFeature())
}
