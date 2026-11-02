@file:OptIn(MapsComposeExperimentalApi::class)

package com.achtien.codingtemplate.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.achtien.codingtemplate.ui.components.OverlayCardSheet
import com.achtien.codingtemplate.utils.openAppSettings
import com.achtien.codingtemplate.utils.openLocationSettings
import com.achtien.codingtemplate.utils.toLatLng
import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.singleFeature
import com.achtien.repository.EarthquakeResults
import com.achtien.repository.location.UserLocation
import com.achtien.repository.location.hasPermission
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Clock

private const val WORLD_ZOOM = 2f
private const val REGION_ZOOM = 7f
private const val DETAIL_ZOOM = 10f

@Composable
fun EarthquakeMapScreen(
    viewModel: EarthquakeMapViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EarthquakeMapContent(
        state = state,
        userLocation = userLocation,
        selected = selected,
        onRegionChanged = viewModel::onRegionChanged,
        onRefresh = viewModel::refreshEarthquakes,
        onSelect = viewModel::select,
        onOpenAppSettings = context::openAppSettings,
        onOpenLocationSettings = context::openLocationSettings,
    )
}

@Composable
private fun EarthquakeMapContent(
    state: EarthquakeMapState,
    userLocation: UserLocation,
    selected: EarthquakeFeature?,
    onRegionChanged: (GeoBounds) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (EarthquakeFeature?) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    Scaffold(
        // No top bar while details are open
        topBar = { if (selected == null) EarthquakeTopBar(onRefresh) },
    ) { padding ->
        // World view until the user's position is known
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), WORLD_ZOOM)
        }
        ZoomToUserLocation(cameraPositionState, userLocation)
        FlyToEarthquake(cameraPositionState, selected)
        val reportRegion = rememberRegionReporter(cameraPositionState, onRegionChanged)

        val results = (state as? EarthquakeMapState.Loaded)?.results
        val listState = rememberLazyListState()
        LaunchedEffect(results?.earthquakes) { listState.scrollToItem(0) }

        BackHandler(enabled = selected != null) { onSelect(null) }

        OverlayCardSheet(
            modifier = Modifier.padding(padding),
            // The details card stays collapsed; only the list expands.
            expandable = selected == null,
            sheetContent = {
                if (selected != null) {
                    EarthquakeDetails(selected, userLocation, { onSelect(null) }, onOpenAppSettings, onOpenLocationSettings)
                } else {
                    EarthquakeRegionList(state, userLocation, listState, onRefresh, onSelect)
                }
            },
        ) { mapPadding ->
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = mapPadding,
                properties = MapProperties(
                    isMyLocationEnabled = userLocation.hasPermission,
                    minZoomPreference = WORLD_ZOOM,
                ),
                onMapLoaded = reportRegion,
            ) {
                EarthquakeMarkers(results, selected, cameraPositionState, onSelect)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EarthquakeTopBar(onRefresh: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Earthquakes") },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        },
    )
}

@Composable
private fun ZoomToUserLocation(
    cameraPositionState: CameraPositionState,
    userLocation: UserLocation,
) {
    LaunchedEffect(userLocation) {
        val known = userLocation as? UserLocation.Known ?: return@LaunchedEffect
        cameraPositionState.flyTo(known.toLatLng(), REGION_ZOOM)
    }
}

@Composable
private fun FlyToEarthquake(
    cameraPositionState: CameraPositionState,
    selected: EarthquakeFeature?,
) {
    var cameraBeforeSelection by rememberSaveable { mutableStateOf<CameraPosition?>(null) }
    LaunchedEffect(selected?.id) {
        if (selected != null) {
            if (cameraBeforeSelection == null) cameraBeforeSelection = cameraPositionState.position
            cameraPositionState.flyTo(selected.geometry.toLatLng(), maxOf(cameraPositionState.position.zoom, DETAIL_ZOOM))
        } else {
            cameraBeforeSelection?.let { cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(it)) }
            cameraBeforeSelection = null
        }
    }
}

/**
 * Reports the visible region whenever the camera settles. Also returned, for the map's load: there is
 * no projection to read before then.
 */
@Composable
private fun rememberRegionReporter(
    cameraPositionState: CameraPositionState,
    onRegionChanged: (GeoBounds) -> Unit,
): () -> Unit {
    val currentOnRegionChanged by rememberUpdatedState(onRegionChanged)
    val reportRegion = remember(cameraPositionState) {
        {
            // The visible region already excludes the map's content padding (the collapsed sheet).
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let {
                currentOnRegionChanged(it.toGeoBounds())
            }
            Unit
        }
    }
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }.filter { !it }
            .collect { reportRegion() }
    }
    return reportRegion
}

@Composable
@GoogleMapComposable
private fun EarthquakeMarkers(
    results: EarthquakeResults?,
    selected: EarthquakeFeature?,
    cameraPositionState: CameraPositionState,
    onSelect: (EarthquakeFeature) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val showAll = selected == null
    val items = remember(results, showAll) {
        if (showAll) results?.earthquakes.orEmpty().map(::EarthquakeClusterItem) else emptyList()
    }
    Clustering(
        items = items,
        onClusterClick = { cluster ->
            scope.launch {
                cameraPositionState.flyTo(cluster.position, cameraPositionState.position.zoom + 2)
            }
            true
        },
        onClusterItemClick = { item ->
            onSelect(item.quake)
            true
        },
    )
    if (selected != null) {
        Marker(state = rememberUpdatedMarkerState(selected.geometry.toLatLng()), onClick = { true })
    }
}

@Preview
@Composable
private fun PreviewEarthquakeMapScreen() {
    EarthquakeMapContent(
        state = EarthquakeMapState.Loaded(
            results = EarthquakeResults(
                query = GeoBounds(36.5, -123.5, 38.5, -121.0).toEarthquakeQuery(Clock.System.now()),
                earthquakes = List(3) { singleFeature.copy(id = "$it") },
                fetchedAt = Clock.System.now(),
            ),
            isRefreshing = false,
            refreshError = null,
        ),
        userLocation = UserLocation.Known(37.77, -122.42),
        selected = null,
        onRegionChanged = {},
        onRefresh = {},
        onSelect = {},
        onOpenAppSettings = {},
        onOpenLocationSettings = {},
    )
}

private suspend fun CameraPositionState.flyTo(
    target: LatLng,
    zoom: Float,
) = animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
