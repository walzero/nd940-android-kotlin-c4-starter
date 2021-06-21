package com.udacity.project4.locationreminders.geofence

import com.google.android.gms.location.Geofence

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
}