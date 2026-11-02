package com.achtien.codingtemplate.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.achtien.networking.EarthquakeFeature
import com.achtien.repository.EarthquakeRepository
import com.achtien.repository.EarthquakeResults
import com.achtien.repository.location.UserLocation
import com.achtien.repository.location.UserLocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

sealed interface EarthquakeMapState {
    data object Loading : EarthquakeMapState

    data class Failed(
        val message: String,
    ) : EarthquakeMapState

    data class Loaded(
        val results: EarthquakeResults,
        val isRefreshing: Boolean,
        val refreshError: String?,
    ) : EarthquakeMapState
}

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class EarthquakeMapViewModel(
    repository: EarthquakeRepository,
    userLocationRepository: UserLocationRepository,
) : ViewModel() {
    val userLocation: StateFlow<UserLocation> = userLocationRepository.location

    private val _selected = MutableStateFlow<EarthquakeFeature?>(null)
    val selected: StateFlow<EarthquakeFeature?> = _selected.asStateFlow()

    // The region to show. Bumping [refresh] re-runs it, where setting equal bounds again would be ignored.
    private data class Request(
        val bounds: GeoBounds,
        val refresh: Int = 0,
    )

    private val request = MutableStateFlow<Request?>(null)

    val state: StateFlow<EarthquakeMapState> =
        request
            .filterNotNull()
            .flatMapLatest { repository.observe(it.bounds.toEarthquakeQuery(Clock.System.now())) }
            .map { load ->
                val error = load.error?.let { it.message ?: "Couldn't load earthquakes" }
                when {
                    load.results.fetchedAt != null -> EarthquakeMapState.Loaded(load.results, load.isLoading, error)
                    error != null -> EarthquakeMapState.Failed(error)
                    else -> EarthquakeMapState.Loading
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, EarthquakeMapState.Loading)

    // null closes the details
    fun select(quake: EarthquakeFeature?) {
        _selected.value = quake
    }

    // Ignored while an earthquake is selected: the camera is on it, not on a region to list.
    fun onRegionChanged(bounds: GeoBounds) {
        if (_selected.value != null) return
        if (request.value?.bounds != bounds) request.value = Request(bounds)
    }

    fun refreshEarthquakes() = request.update { it?.copy(refresh = it.refresh + 1) }
}
