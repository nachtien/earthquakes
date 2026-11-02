package com.achtien.codingtemplate.utils

import com.achtien.networking.PointGeometry
import com.achtien.repository.location.UserLocation
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

fun PointGeometry.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}

fun UserLocation.Known.toLatLng(): LatLng = LatLng(latitude, longitude)

/** Great-circle distance in km. */
fun UserLocation.Known.distanceKmTo(geometry: PointGeometry): Double =
    SphericalUtil.computeDistanceBetween(toLatLng(), geometry.toLatLng()) / 1000

/** Great-circle distance in km, or null when the user's position isn't known. */
fun UserLocation.distanceKmTo(geometry: PointGeometry): Double? = (this as? UserLocation.Known)?.distanceKmTo(geometry)
