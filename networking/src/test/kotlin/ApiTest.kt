package com.achtien.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ApiTest {
    private val query =
        EarthquakeQuery(
            minLatitude = -10.0,
            maxLatitude = 10.0,
            minLongitude = 170.0,
            maxLongitude = 190.0,
            startTime = Instant.parse("2026-09-30T12:00:00Z"),
            minMagnitude = 2.5,
            limit = 500,
        )

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = EMPTY_FDSN_RESPONSE,
        onRequest: (HttpRequestData) -> Unit = {},
    ): HttpClient {
        val engine =
            MockEngine { request ->
                onRequest(request)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        return createHttpClient(engine, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `query sends bounds, window, magnitude and limit to the fdsn endpoint`() =
        runTest {
            lateinit var request: HttpRequestData
            client { request = it }.queryEarthquakes(query)

            assertEquals("earthquake.usgs.gov", request.url.host)
            assertEquals("/fdsnws/event/1/query", request.url.encodedPath)
            val params = request.url.parameters
            assertEquals("geojson", params["format"])
            assertEquals("time", params["orderby"])
            assertEquals("-10.0", params["minlatitude"])
            assertEquals("10.0", params["maxlatitude"])
            assertEquals("170.0", params["minlongitude"])
            assertEquals("190.0", params["maxlongitude"])
            assertEquals("2026-09-30T12:00:00", params["starttime"])
            assertEquals("2.5", params["minmagnitude"])
            assertEquals("500", params["limit"])
        }

    @Test
    fun `parses a feature collection`() =
        runTest {
            val feed = javaClass.getResource("/all_day.geojson")!!.readText()
            val quakes = client(body = feed).queryEarthquakes(query)
            assertEquals(213, quakes.size)
            assertEquals("ci41570024", quakes.first().id)
        }

    @Test
    fun `parses an fdsn response with limit and no count`() =
        runTest {
            assertTrue(client().queryEarthquakes(query).isEmpty())
        }

    @Test
    fun `no content means no earthquakes`() =
        runTest {
            assertTrue(client(status = HttpStatusCode.NoContent, body = "").queryEarthquakes(query).isEmpty())
        }

    private companion object {
        // Verbatim shape of a live FDSN response with `limit` set: metadata has limit/offset, no count.
        const val EMPTY_FDSN_RESPONSE =
            """{"type":"FeatureCollection","metadata":{"generated":1793416525000,""" +
                """"url":"https://earthquake.usgs.gov/fdsnws/event/1/query","title":"USGS Earthquakes",""" +
                """"status":200,"api":"2.8.2","limit":500,"offset":1},"features":[]}"""
    }
}
