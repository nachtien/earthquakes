package com.achtien.repository.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@Single
class FusedLocationProvider(
    private val context: Context,
) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    override suspend fun currentLocation(): UserLocation {
        if (!hasPermission()) return UserLocation.PermissionDenied
        val locationManager = context.getSystemService<LocationManager>()
        if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
            return UserLocation.ServicesOff
        }
        // A fresh fix, cancelled along with the coroutine; the OS's cached fix as a fallback.
        val fix =
            orNull {
                withTimeoutOrNull(FIX_TIMEOUT) {
                    val cancellation = CancellationTokenSource()
                    client
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                        .await(cancellation)
                }
            } ?: orNull { client.lastLocation.await() }
        return fix?.let { UserLocation.Known(it.latitude, it.longitude) } ?: UserLocation.Unavailable
    }

    /** Like runCatching, but never swallows cancellation. */
    private inline fun <T> orNull(block: () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        val FIX_TIMEOUT = 10.seconds
    }
}
