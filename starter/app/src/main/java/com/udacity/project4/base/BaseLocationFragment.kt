package com.udacity.project4.base

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.utils.*


abstract class BaseLocationFragment : BaseFragment() {

    private val TAG = BaseLocationFragment::class.simpleName

    val _locationServices by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

    @RequiresApi(Build.VERSION_CODES.Q)
    private val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    private lateinit var permissionDialog: MaterialDialog

    open fun onLocationPermissionsGranted() {}
    open fun onLocationPermissionsDenied() {}

    fun requestLocationPermissions() {
        if (hasPermissions()) {
            onLocationPermissionsGranted()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(30)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (canRequestFineLocationPermission()) {
            locationPermissionsRequest.launch(fineLocationPermission)
            return
        }

        if (canRequestBackgroundLocationPermission()) {
            locationPermissionsRequest.launch(backgroundPermission)
            return
        }

        permissionDialog = showPermissionsRequiredDialog {
            requireActivity().launchPermissionSettingsActivity()
        }
    }

    private fun canRequestBackgroundLocationPermission() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                requireContext().isAllowed(fineLocationPermission) &&
                shouldShowRequestPermissionRationale(backgroundPermission)

    private fun canRequestFineLocationPermission() =
        !requireContext().isAllowed(fineLocationPermission) &&
                shouldShowRequestPermissionRationale(fineLocationPermission)

    fun hasPermissions(): Boolean {
        var requiredPermissions = arrayOf(fineLocationPermission)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            requiredPermissions += backgroundPermission

        return requireContext().areAllowed(requiredPermissions)
    }

    private var locationPermissionsRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (!result) {
                permissionDialog = showPermissionsRequiredDialog { requestLocationPermissions() }
                return@registerForActivityResult
            } else {
                requestLocationPermissions()
            }
        }

    private fun showPermissionsRequiredDialog(
        onNegativeAction: () -> Unit = { goBackToLastFragment() },
        onPositiveAction: () -> Unit
    ): MaterialDialog = requireActivity().showYesNoDialog(
        title = R.string.permission_denied_title,
        message = R.string.permission_denied_explanation,
        positiveText = R.string.allow,
        negativeText = R.string.go_back,
        onNegativeAction = onNegativeAction,
        onPositiveAction = onPositiveAction
    )

    private fun goBackToLastFragment() {
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onDetach() {
        super.onDetach()
        dismissCurrentDialog()
    }

    private fun dismissCurrentDialog() {
        if (::permissionDialog.isInitialized && permissionDialog.isShowing)
            permissionDialog.tryDimiss()
    }
}