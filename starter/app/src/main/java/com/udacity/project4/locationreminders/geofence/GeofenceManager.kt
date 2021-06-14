package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun createGeofences(
        requestLocationPermissions: () -> Unit,
        hasPermissions: () -> Boolean,
        onSuccessListener: (List<String>) -> Unit,
        onFailureListener: (Exception) -> Unit,
        vararg geofences: Geofence?
    ) {
        monitorGeofences(
            requestLocationPermissions = requestLocationPermissions,
            hasPermissions = hasPermissions,
            onSuccessListener = onSuccessListener,
            onFailureListener = onFailureListener,
            geofences = geofences.filterNotNull()
        )
    }

    private fun monitorGeofences(
        requestLocationPermissions: () -> Unit,
        hasPermissions: () -> Boolean,
        onSuccessListener: (List<String>) -> Unit = {},
        onFailureListener: (Exception) -> Unit = {},
        geofences: List<Geofence>
    ) {
        requestLocationPermissions()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        if (hasPermissions())
            handleGeofencingRequest(
                geofencingRequest = geofencingRequest,
                onSuccessListener = onSuccessListener,
                onFailureListener = onFailureListener
            )
    }

    @SuppressLint("MissingPermission")
    private fun handleGeofencingRequest(
        geofencingRequest: GeofencingRequest,
        onSuccessListener: (List<String>) -> Unit = {},
        onFailureListener: (Exception) -> Unit = {}
    ) {
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener { onSuccessListener(geofencingRequest.geofences.map { it.requestId }) }
            addOnFailureListener { onFailureListener(it) }
        }
    }
}