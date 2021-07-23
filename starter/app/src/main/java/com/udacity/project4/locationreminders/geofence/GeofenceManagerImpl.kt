package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class GeofenceManagerImpl(private val application: Application) : GeofenceManager {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(application)
    }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(application, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(application, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun verifyLocationSettings(
        locationSettings: SettingsClient,
        onSuccessListener: (LocationSettingsResponse?) -> Unit,
        onFailureListener: (Exception) -> Unit
    ) {
        val requestBuilder = LocationSettingsRequest.Builder().apply {
            addLocationRequest(LocationRequest.create())
        }

        locationSettings.checkLocationSettings(requestBuilder.build()).run {
            addOnSuccessListener { response -> onSuccessListener(response) }
            addOnFailureListener { exception -> onFailureListener(exception) }
        }
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