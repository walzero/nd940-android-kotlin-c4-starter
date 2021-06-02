package com.udacity.project4.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.utils.showYesNoDialog

abstract class BaseLocationFragment : BaseFragment() {

    val _locationServices by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val baseRequiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    private lateinit var permissionDialog: MaterialDialog

    abstract fun onLocationPermissionsGranted()
    abstract fun onLocationPermissionsDenied()

    fun requestLocationPermissions() {
        requestBasePermission.launch(baseRequiredPermissions)
    }

    fun hasPermissions(): Boolean {
        var hasAllPermissions = baseRequiredPermissions.checkIfHasAllPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasAllPermissions = hasAllPermissions && arrayOf(backgroundPermission).checkIfHasAllPermission()
        }

        return hasAllPermissions
    }

    private fun Array<String>.checkIfHasAllPermission(): Boolean {
        forEach { permission ->
            if (getPermissionStatus(permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        return true
    }

    private var requestBasePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                Log.i("DEBUG", "permissions granted")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundPermission.launch(backgroundPermission)
                } else {
                    onLocationPermissionsGranted()
                }

            } else {
                Log.i("DEBUG", "permissions denied: ${result.filter { it.value }}")
                onLocationPermissionsDenied()
                permissionDialog = showPermissionsRequiredDialog()
            }
        }

    private var requestBackgroundPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                Log.i("DEBUG", "permissions granted")
                onLocationPermissionsGranted()
            } else {
                Log.i("DEBUG", "permissions denied: $result")
                onLocationPermissionsDenied()
                permissionDialog = showPermissionsRequiredDialog()
            }
        }

    private fun showPermissionsRequiredDialog(
        onNegativeAction: () -> Unit = { goBackToLastFragment() },
        onPositiveAction: () -> Unit = { requestLocationPermissions() }
    ): MaterialDialog = requireActivity().showYesNoDialog(
        title = R.string.permission_denied_title,
        message = R.string.permission_denied_explanation,
        positiveText = R.string.try_again,
        negativeText = R.string.go_back,
        onNegativeAction = onNegativeAction,
        onPositiveAction = onPositiveAction
    )

    private fun goBackToLastFragment() {
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun getPermissionStatus(permission: String) =
        ActivityCompat.checkSelfPermission(requireContext(), permission)
}