package com.achtien.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

suspend fun HttpClient.queryEarthquakes(query: EarthquakeQuery): List<EarthquakeFeature> {
    val response =
        get("fdsnws/event/1/query") {
            parameter("format", "geojson")
            parameter("orderby", "time")
            parameter("minlatitude", query.minLatitude)
            parameter("maxlatitude", query.maxLatitude)
            parameter("minlongitude", query.minLongitude)
            parameter("maxlongitude", query.maxLongitude)
            parameter(
                "starttime",
                query.startTime.toLocalDateTime(TimeZone.UTC).format(LocalDateTime.Formats.ISO),
            )
            parameter("minmagnitude", query.minMagnitude)
            parameter("limit", query.limit)
        }
    if (response.status == HttpStatusCode.NoContent) return emptyList()
    return response.body<EarthquakeFeed>().features
}
