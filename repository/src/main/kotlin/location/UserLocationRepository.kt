package com.achtien.repository.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/** The single, app-wide source of the user's location. Screens read [location]; only the Activity refreshes. */
@Single
class UserLocationRepository(
    private val provider: LocationProvider,
    private val scope: CoroutineScope,
) {
    private val _location = MutableStateFlow<UserLocation>(UserLocation.Unknown)
    val location: StateFlow<UserLocation> = _location.asStateFlow()

    /**
     * Re-checks the permission synchronously (so the map never enables its location layer without it)
     * and fetches a fresh fix. The last known value is kept until the new one arrives; if two fetches
     * overlap (permission result and onResume), the last to finish wins.
     */
    fun refresh() {
        if (!provider.hasPermission()) {
            _location.value = UserLocation.PermissionDenied
            return
        }
        scope.launch { _location.value = provider.currentLocation() }
    }
}
