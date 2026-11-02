package com.achtien.repository

import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.EarthquakeFeed
import com.achtien.networking.EarthquakeQuery
import com.achtien.networking.PointGeometry
import com.achtien.networking.createHttpClient
import com.achtien.networking.singleFeature
import com.achtien.repository.cache.EarthquakeDatabase
import com.achtien.repository.cache.inMemoryDatabase
import com.achtien.repository.cache.toFeature
import com.achtien.repository.cache.toRow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class EarthquakeRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val now = Instant.parse("2026-10-31T12:00:00Z")
    private val earlier = now - 2.hours
    private val clock = object : Clock {
        override fun now() = now
    }
    private val database = inMemoryDatabase()
    private val query =
        EarthquakeQuery(
            minLatitude = 30.0,
            maxLatitude = 40.0,
            minLongitude = -125.0,
            maxLongitude = -120.0,
            startTime = now - 30.days,
            minMagnitude = 0.0,
            limit = 500,
        )

    /** In the query's box, [hoursAgo] before [now]. */
    private fun quake(
        id: String,
        hoursAgo: Int = 1,
        longitude: Double = -122.0,
    ) = singleFeature.copy(
        id = id,
        properties = singleFeature.properties.copy(time = now - hoursAgo.hours),
        geometry = PointGeometry(listOf(longitude, 35.0, 1.0)),
    )

    private fun EarthquakeDatabase.seed(vararg rows: Pair<EarthquakeFeature, Instant>) =
        rows.forEach { (feature, fetchedAt) -> earthquakeQueries.upsert(feature.toRow(fetchedAt)) }

    private fun EarthquakeDatabase.ids() =
        earthquakeQueries
            .selectMatching(0.0, Instant.DISTANT_PAST, -90.0, 90.0, -180.0, 540.0, Long.MAX_VALUE)
            .executeAsList()
            .map { it.id }
            .toSet()

    /** USGS answering with [features], or unreachable when null. */
    private fun TestScope.repository(features: List<EarthquakeFeature>?) =
        UsgsEarthquakeRepository(
            createHttpClient(
                MockEngine {
                    features ?: throw IOException("offline")
                    respond(json.encodeToString(EarthquakeFeed(features)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
                },
                json,
            ),
            database,
            clock,
            UnconfinedTestDispatcher(testScheduler),
        )

    /** Every emission of loading [query] until the refresh has settled; the flow itself never completes. */
    private suspend fun loads(
        repository: UsgsEarthquakeRepository,
        query: EarthquakeQuery = this.query,
    ) = repository
        .observe(query)
        .transformWhile {
            emit(it)
            it.isLoading
        }.toList()

    @Test
    fun `with nothing stored, emits an empty loading state then USGS's answer and stores it`() =
        runTest {
            val fresh = listOf(quake("a"))
            val loads = loads(repository(fresh))

            assertEquals(EarthquakeLoad(EarthquakeResults(query, emptyList(), fetchedAt = null), isLoading = true), loads.first())
            assertEquals(EarthquakeLoad(EarthquakeResults(query, fresh, fetchedAt = now), isLoading = false), loads.last())
            assertEquals(setOf("a"), database.ids())
        }

    @Test
    fun `the database answers with only what matches, newest first, as fresh as its newest`() =
        runTest {
            database.seed(quake("old", hoursAgo = 5) to now - 5.hours, quake("new") to earlier, quake("elsewhere", longitude = 10.0) to now)
            val first = loads(repository(emptyList())).first()

            assertEquals(listOf("new", "old"), first.results.earthquakes.map { it.id })
            assertEquals(earlier, first.results.fetchedAt)
            assertTrue(first.isLoading)
        }

    @Test
    fun `when USGS can't be reached, the stored results come with the error`() =
        runTest {
            database.seed(quake("a") to earlier)
            val loads = loads(repository(features = null))

            assertEquals(loads.first().results, loads.last().results)
            assertFalse(loads.last().isLoading)
            assertTrue(loads.last().error is IOException)
        }

    @Test
    fun `fresh results replace stored ones by id and keep the rest`() =
        runTest {
            database.seed(quake("a") to earlier, quake("b") to earlier)
            val updated = quake("a").let { it.copy(properties = it.properties.copy(mag = 5.0)) }
            loads(repository(listOf(updated)))

            val rows = database.earthquakeQueries.selectMatching(0.0, query.startTime, 30.0, 40.0, -125.0, -120.0, 10).executeAsList()
            assertEquals(setOf(updated to now, quake("b") to earlier), rows.map { it.toFeature() to it.fetchedAt }.toSet())
        }

    @Test
    fun `earthquakes older than the query's window are dropped on save`() =
        runTest {
            database.seed(quake("ancient", hoursAgo = 31 * 24) to earlier)
            loads(repository(listOf(quake("a"))))
            assertEquals(setOf("a"), database.ids())
        }

    @Test
    fun `the database answers with at most the query's limit, newest first`() =
        runTest {
            database.seed(*Array(3) { quake("$it", hoursAgo = it + 1) to earlier })
            val first = loads(repository(emptyList()), query.copy(limit = 2)).first()
            assertEquals(listOf("0", "1"), first.results.earthquakes.map { it.id })
        }

    @Test
    fun `an empty answer from USGS is fresh, so the UI can say there are none`() =
        runTest {
            val last = loads(repository(emptyList())).last()
            assertEquals(EarthquakeResults(query, emptyList(), fetchedAt = now), last.results)
            assertNull(last.error)
        }

    @Test
    fun `a query sees what another query's refresh wrote`() =
        runTest {
            val seenByA = repository(emptyList()).observe(query).map { load -> load.results.earthquakes.map { it.id } }
            val a = async { seenByA.first { it.isNotEmpty() } }
            // A wider query whose answer includes a quake inside A's box.
            loads(repository(listOf(quake("b"))), query.copy(minLatitude = 0.0))
            assertEquals(listOf("b"), a.await())
        }
}
