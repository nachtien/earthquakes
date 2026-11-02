package com.achtien.codingtemplate.map

import com.achtien.codingtemplate.utils.toLatLng
import com.achtien.networking.EarthquakeFeature
import com.google.maps.android.clustering.ClusterItem

class EarthquakeClusterItem(
    val quake: EarthquakeFeature,
) : ClusterItem {
    override val position = quake.geometry.toLatLng()
    override val title: String? = null
    override val snippet = null
    override val zIndex = null
}
