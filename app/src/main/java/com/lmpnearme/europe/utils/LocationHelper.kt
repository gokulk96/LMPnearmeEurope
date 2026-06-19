package com.lmpnearme.europe.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun detectZone(): BiddingZone? {
        // Request a fresh fix first; last-known location may be stale (different country)
        val location = getCurrentLocation() ?: getLastLocation() ?: return null
        val countryCode = reverseGeocode(location.first, location.second) ?: return null
        return BiddingZoneMapper.forCountryCode(countryCode)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(if (loc != null) Pair(loc.latitude, loc.longitude) else null)
                }
                .addOnFailureListener { cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    cont.resume(if (loc != null) Pair(loc.latitude, loc.longitude) else null)
                }
                .addOnFailureListener { cont.resume(null) }
        }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        cont.resume(addresses.firstOrNull()?.countryCode)
                    }
                }
            } else {
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.countryCode
            }
        } catch (_: Exception) { null }
    }
}
