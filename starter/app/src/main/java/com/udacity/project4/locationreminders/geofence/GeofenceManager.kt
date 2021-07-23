package com.udacity.project4.locationreminders.geofence

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient

interface GeofenceManager {
    fun createGeofences(
        hasPermissions: () -> Boolean = { true },
        onSuccessListener: (List<String>) -> Unit,
        onFailureListener: (Exception) -> Unit,
        vararg geofences: Geofence?
    )

    fun createGeofences(
        hasPermissions: () -> Boolean = { true },
        vararg geofences: Geofence?
    )

    fun disableAllGeofences()

    fun monitorGeofences(
        hasPermissions: () -> Boolean,
        onSuccessListener: (List<String>) -> Unit = {},
        onFailureListener: (Exception) -> Unit = {},
        geofences: List<Geofence>
    )

    fun verifyLocationSettings(
        locationSettings: SettingsClient,
        onSuccessListener: (LocationSettingsResponse?) -> Unit,
        onFailureListener: (Exception) -> Unit
    )
}