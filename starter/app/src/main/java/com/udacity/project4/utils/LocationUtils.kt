package com.udacity.project4.utils

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION

@RequiresApi(Build.VERSION_CODES.Q)
val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION