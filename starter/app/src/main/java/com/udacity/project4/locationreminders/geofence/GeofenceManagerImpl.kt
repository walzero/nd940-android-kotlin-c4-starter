package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManagerImpl(private val application: Application) : GeofenceManager {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(application)
    }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(application, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(application, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createGeofences(
        hasPermissions: () -> Boolean,
        onSuccessListener: (List<String>) -> Unit,
        onFailureListener: (Exception) -> Unit,
        vararg geofences: Geofence?
    ) = monitorGeofences(
        hasPermissions = hasPermissions,
        onSuccessListener = onSuccessListener,
        onFailureListener = onFailureListener,
        geofences = geofences.filterNotNull()
    )

    override fun createGeofences(
        hasPermissions: () -> Boolean,
        vararg geofences: Geofence?
    ) = monitorGeofences(
        geofences = geofences.filterNotNull(),
        hasPermissions = hasPermissions
    )

    override fun disableAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    override fun monitorGeofences(
        hasPermissions: () -> Boolean,
        onSuccessListener: (List<String>) -> Unit,
        onFailureListener: (Exception) -> Unit,
        geofences: List<Geofence>
    ) {
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
            addOnSuccessListener {
                Log.d("GEOFENCE", "ADDED WITH SUCCESS: ${geofencingRequest.geofences}")
                onSuccessListener(geofencingRequest.geofences.map { it.requestId })
            }
            addOnFailureListener {
                Log.d("GEOFENCE", "FAILED TO ADD")
                onFailureListener(it)
            }
        }
    }
}