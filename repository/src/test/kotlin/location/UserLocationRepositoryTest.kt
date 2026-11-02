package com.achtien.repository.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserLocationRepositoryTest {
    private class FakeLocationProvider(
        var permission: Boolean = true,
        var result: UserLocation = UserLocation.Known(1.0, 2.0),
    ) : LocationProvider {
        var calls = 0

        /** Replace with an incomplete one to hold the next fetch. */
        var gate = CompletableDeferred(Unit)

        override fun hasPermission() = permission

        override suspend fun currentLocation(): UserLocation {
            calls++
            gate.await()
            return result
        }
    }

    @Test
    fun `starts unknown`() =
        runTest {
            val repo = UserLocationRepository(FakeLocationProvider(), this)
            assertEquals(UserLocation.Unknown, repo.location.value)
        }

    @Test
    fun `without permission reports denied immediately and never fetches`() =
        runTest {
            val fake = FakeLocationProvider(permission = false)
            val repo = UserLocationRepository(fake, this)
            repo.refresh()
            assertEquals(UserLocation.PermissionDenied, repo.location.value)
            advanceUntilIdle()
            assertEquals(0, fake.calls)
        }

    @Test
    fun `with permission publishes the fix`() =
        runTest {
            val fake = FakeLocationProvider()
            val repo = UserLocationRepository(fake, this)
            repo.refresh()
            advanceUntilIdle()
            assertEquals(UserLocation.Known(1.0, 2.0), repo.location.value)
        }

    @Test
    fun `keeps the last fix while refreshing`() =
        runTest {
            val fake = FakeLocationProvider()
            val repo = UserLocationRepository(fake, this)
            repo.refresh()
            advanceUntilIdle()
            fake.gate = CompletableDeferred()
            fake.result = UserLocation.Known(3.0, 4.0)
            repo.refresh()
            advanceUntilIdle()
            assertEquals(UserLocation.Known(1.0, 2.0), repo.location.value)
            fake.gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(UserLocation.Known(3.0, 4.0), repo.location.value)
        }

    @Test
    fun `revoked permission replaces a known fix`() =
        runTest {
            val fake = FakeLocationProvider()
            val repo = UserLocationRepository(fake, this)
            repo.refresh()
            advanceUntilIdle()
            fake.permission = false
            repo.refresh()
            assertEquals(UserLocation.PermissionDenied, repo.location.value)
        }

    @Test
    fun `provider failures are surfaced as-is`() =
        runTest {
            val fake = FakeLocationProvider(result = UserLocation.ServicesOff)
            val repo = UserLocationRepository(fake, this)
            repo.refresh()
            advanceUntilIdle()
            assertEquals(UserLocation.ServicesOff, repo.location.value)
            fake.result = UserLocation.Unavailable
            repo.refresh()
            advanceUntilIdle()
            assertEquals(UserLocation.Unavailable, repo.location.value)
        }
}
