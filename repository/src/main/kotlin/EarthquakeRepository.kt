package com.achtien.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.EarthquakeQuery
import com.achtien.networking.queryEarthquakes
import com.achtien.repository.cache.Earthquake
import com.achtien.repository.cache.EarthquakeDatabase
import com.achtien.repository.cache.toFeature
import com.achtien.repository.cache.toRow
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

interface EarthquakeRepository {
    fun observe(query: EarthquakeQuery): Flow<EarthquakeLoad>
}

data class EarthquakeResults(
    val query: EarthquakeQuery,
    val earthquakes: List<EarthquakeFeature>,
    val fetchedAt: Instant?,
)

data class EarthquakeLoad(
    val results: EarthquakeResults,
    val isLoading: Boolean,
    val error: Exception? = null,
)

private sealed interface Fetch {
    data object Running : Fetch

    data class Done(
        val at: Instant,
    ) : Fetch

    data class Failed(
        val error: Exception,
    ) : Fetch
}

@Single
class UsgsEarthquakeRepository(
    private val httpClient: HttpClient,
    private val database: EarthquakeDatabase,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher,
) : EarthquakeRepository {

    private val queries get() = database.earthquakeQueries

    override fun observe(query: EarthquakeQuery): Flow<EarthquakeLoad> =
        combine(quakes(query), fetch(query)) { rows, fetch ->
            val fetchedAt = listOfNotNull(
                rows.maxOfOrNull { it.fetchedAt },
                (fetch as? Fetch.Done)?.at,
            ).maxOrNull()
            EarthquakeLoad(
                EarthquakeResults(query, rows.map { it.toFeature() }, fetchedAt),
                isLoading = fetch is Fetch.Running,
                error = (fetch as? Fetch.Failed)?.error,
            )
        }

    private fun quakes(query: EarthquakeQuery): Flow<List<Earthquake>> =
        queries
            .selectMatching(
                query.minMagnitude,
                query.startTime,
                query.minLatitude,
                query.maxLatitude,
                query.minLongitude,
                query.maxLongitude,
                query.limit.toLong(),
            ).asFlow()
            .mapToList(ioDispatcher)

    private fun fetch(query: EarthquakeQuery): Flow<Fetch> =
        flow {
            emit(Fetch.Running)
            val fetch =
                try {
                    val fresh = httpClient.queryEarthquakes(query)
                    val now = clock.now()
                    withContext(ioDispatcher) { insert(fresh, now, query) }
                    Fetch.Done(now)
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    Fetch.Failed(e)
                }
            emit(fetch)
        }

    private fun insert(
        fresh: List<EarthquakeFeature>,
        now: Instant,
        query: EarthquakeQuery,
    ) = queries.transaction {
        fresh.forEach { queries.upsert(it.toRow(now)) }
        queries.deleteOlderThan(query.startTime)
    }
}
