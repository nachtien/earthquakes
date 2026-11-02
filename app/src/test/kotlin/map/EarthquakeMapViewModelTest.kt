package com.achtien.codingtemplate.map

import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.EarthquakeQuery
import com.achtien.networking.singleFeature
import com.achtien.repository.EarthquakeLoad
import com.achtien.repository.EarthquakeRepository
import com.achtien.repository.EarthquakeResults
import com.achtien.repository.location.LocationProvider
import com.achtien.repository.location.UserLocation
import com.achtien.repository.location.UserLocationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EarthquakeMapViewModelTest {
    /** Emits [cached] at once, then waits on [gate] and emits [result] or [cached] with [failure]. */
    private class FakeEarthquakeRepository : EarthquakeRepository {
        val queries = mutableListOf<EarthquakeQuery>()
        var failure: Exception? = null
        var cached: List<EarthquakeFeature>? = null

        /** Each call waits on the gate current at call time; replace it to hold a later call. */
        var gate = CompletableDeferred(Unit)
        var result: (EarthquakeQuery) -> List<EarthquakeFeature> = { listOf(singleFeature) }

        override fun observe(query: EarthquakeQuery): Flow<EarthquakeLoad> {
            queries += query
            val (gate, result, failure) = Triple(gate, result, failure)
            val cached = EarthquakeResults(query, cached.orEmpty(), cached?.let { FETCHED_AT })
            return flow {
                emit(EarthquakeLoad(cached, isLoading = true))
                gate.await()
                if (failure != null) {
                    emit(EarthquakeLoad(cached, isLoading = false, error = failure))
                } else {
                    emit(EarthquakeLoad(EarthquakeResults(query, result(query), FETCHED_AT), isLoading = false))
                }
            }
        }
    }

    private object NoLocation : LocationProvider {
        override fun hasPermission() = false

        override suspend fun currentLocation() = UserLocation.PermissionDenied
    }

    private val repository = FakeEarthquakeRepository()
    private val bayArea = GeoBounds(south = 36.5, west = -123.5, north = 38.5, east = -121.0)
    private val tokyo = GeoBounds(south = 35.0, west = 139.0, north = 36.5, east = 140.5)

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = EarthquakeMapViewModel(repository, UserLocationRepository(NoLocation, backgroundScope))

    private val EarthquakeMapViewModel.loaded get() = state.value as EarthquakeMapState.Loaded

    private val EarthquakeMapViewModel.ids get() = loaded.results.earthquakes.map { it.id }

    private fun quake(id: String) = singleFeature.copy(id = id)

    @Test
    fun `starts loading`() =
        runTest {
            assertEquals(EarthquakeMapState.Loading, viewModel().state.value)
        }

    @Test
    fun `a newer region cancels the in-flight query`() =
        runTest {
            val vm = viewModel()
            val staleGate = CompletableDeferred<Unit>()
            repository.gate = staleGate
            repository.result = { listOf(quake("stale")) }
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()

            repository.gate = CompletableDeferred(Unit)
            repository.result = { listOf(quake("fresh")) }
            vm.onRegionChanged(tokyo)
            advanceUntilIdle()

            assertEquals(2, repository.queries.size)
            assertEquals(listOf("fresh"), vm.ids)
            assertFalse(vm.loaded.isRefreshing)

            // Had the first query not been cancelled, releasing it now would overwrite the fresh results.
            staleGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf("fresh"), vm.ids)
        }

    @Test
    fun `cached results show at once while the network loads`() =
        runTest {
            val vm = viewModel()
            repository.cached = listOf(quake("cached"))
            repository.gate = CompletableDeferred()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            assertEquals(listOf("cached"), vm.ids)
            assertTrue(vm.loaded.isRefreshing)
        }

    @Test
    fun `a failure shows the cached results and a refresh clears the error`() =
        runTest {
            val vm = viewModel()
            repository.cached = listOf(quake("cached"))
            repository.failure = RuntimeException("offline")
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            assertEquals("offline", vm.loaded.refreshError)
            assertEquals(listOf("cached"), vm.ids)

            repository.failure = null
            vm.refreshEarthquakes()
            advanceUntilIdle()
            assertNull(vm.loaded.refreshError)
            assertEquals(listOf(singleFeature), vm.loaded.results.earthquakes)
        }

    @Test
    fun `a failure with nothing cached is a failure`() =
        runTest {
            val vm = viewModel()
            repository.failure = RuntimeException("offline")
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            assertEquals(EarthquakeMapState.Failed("offline"), vm.state.value)
        }

    @Test
    fun `refresh repeats the same query`() =
        runTest {
            val vm = viewModel()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            vm.refreshEarthquakes()
            advanceUntilIdle()
            assertEquals(2, repository.queries.size)
            // Same region; only the window's start moves on with the clock.
            assertEquals(repository.queries[0].copy(startTime = repository.queries[1].startTime), repository.queries[1])
        }

    @Test
    fun `reporting the same region again does not query again`() =
        runTest {
            val vm = viewModel()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            assertEquals(1, repository.queries.size)
        }

    @Test
    fun `refresh before any region does nothing`() =
        runTest {
            val vm = viewModel()
            vm.refreshEarthquakes()
            advanceUntilIdle()
            assertTrue(repository.queries.isEmpty())
        }

    @Test
    fun `selecting publishes the earthquake and clearing it closes the details`() =
        runTest {
            val vm = viewModel()
            vm.select(quake("x"))
            assertEquals(quake("x"), vm.selected.value)
            vm.select(null)
            assertNull(vm.selected.value)
        }

    @Test
    fun `region changes are ignored while an earthquake is selected`() =
        runTest {
            val vm = viewModel()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            vm.select(quake("x"))
            vm.onRegionChanged(tokyo)
            advanceUntilIdle()
            assertEquals(1, repository.queries.size)
            assertEquals(listOf(singleFeature.id), vm.ids)

            vm.select(null)
            vm.onRegionChanged(tokyo)
            advanceUntilIdle()
            assertEquals(2, repository.queries.size)
        }

    @Test
    fun `refresh while selected repeats the list's query`() =
        runTest {
            val vm = viewModel()
            vm.onRegionChanged(bayArea)
            advanceUntilIdle()
            vm.select(quake("x"))
            vm.refreshEarthquakes()
            advanceUntilIdle()
            assertEquals(2, repository.queries.size)
            assertEquals(repository.queries[0].maxLatitude, repository.queries[1].maxLatitude, 0.0)
        }

    private companion object {
        val FETCHED_AT = Instant.parse("2026-10-31T12:00:00Z")
    }
}
