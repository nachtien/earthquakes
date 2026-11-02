package com.achtien.repository.location

sealed interface UserLocation {
    data object Unknown : UserLocation
    data object PermissionDenied : UserLocation
    data object ServicesOff : UserLocation
    data object Unavailable : UserLocation
    data class Known(val latitude: Double, val longitude: Double) : UserLocation
}

val UserLocation.hasPermission: Boolean
    get() = this is UserLocation.Known || this is UserLocation.Unavailable || this is UserLocation.ServicesOff

interface LocationProvider {
    fun hasPermission(): Boolean

    suspend fun currentLocation(): UserLocation
}
