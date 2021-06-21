package com.udacity.project4.base

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    private lateinit var permissionDialog: MaterialDialog

    fun runWithPermission(block: () -> Unit = {}) {
        if (hasLocationPermissions()) block() else requestLocationPermissions()
    }

    @TargetApi(30)
    private fun requestLocationPermissions() {
        if (canRequestFineLocationPermission()) {
            locationPermissionsRequest.launch(fineLocationPermission)
            return
        }

        if (canRequestBackgroundLocationPermission()) {
            showPermissionsRequiredDialog {
                locationPermissionsRequest.launch(backgroundPermission)
            }
        }
    }

    private fun canRequestBackgroundLocationPermission() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !requireContext().applicationContext.isAllowed(backgroundPermission)

    private fun canRequestFineLocationPermission() =
        !requireContext().applicationContext.isAllowed(fineLocationPermission) &&
                !shouldShowRequestPermissionRationale(fineLocationPermission)

    fun hasLocationPermissions(): Boolean {
        var requiredPermissions = arrayOf(fineLocationPermission)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            requiredPermissions += backgroundPermission

        return requireContext().areAllowed(requiredPermissions)
    }

    private var locationPermissionsRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (!result) {
                permissionDialog = showPermissionsRequiredDialog(
                    title = R.string.permission_denied_title,
                    message = R.string.permission_denied_explanation,
                    autoDismiss = false,
                ) {
                    requireActivity().launchPermissionSettingsActivity()
                    permissionDialog.dismiss()
                }
                return@registerForActivityResult
            } else {
                requestLocationPermissions()
            }
        }

    private fun showPermissionsRequiredDialog(
        @StringRes title: Int = R.string.permission_required_title,
        @StringRes message: Int = R.string.permission_required_explanation,
        autoDismiss: Boolean = true,
        onNegativeAction: () -> Unit = { goBackToLastFragment() },
        onPositiveAction: () -> Unit
    ): MaterialDialog = requireActivity().showYesNoDialog(
        title = title,
        message = message,
        autoDismiss = autoDismiss,
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