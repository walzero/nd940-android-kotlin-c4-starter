package com.udacity.project4.locationreminders.savereminder

import android.app.Activity
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private val geofenceManager: GeofenceManager by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private val TAG = SaveReminderFragment::class.simpleName

    private val locationServices by lazy { LocationServices.getSettingsClient(requireContext()) }

    //location stuff
    private lateinit var permissionDialog: MaterialDialog
    private val fineLocationRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            when (result) {
                true -> saveReminderWithBackgroundPermission()
                else -> showPermissionRequiredDialog()
            }
        }

    //for some reason background permission cant be requested along with fineLocation, so I need to make a separate
    //call after I get the foreground location for support to newer versions
    private val backgroundLocationRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            when {
                result -> saveReminderWithBackgroundPermission()
                requireActivity().shouldShowBackgroundLocationDialog() ->
                    showPermissionRequiredDialog()
            }
        }

    private val gpsRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when(result.resultCode) {
                Activity.RESULT_OK -> _viewModel.validateAndSaveReminder()
                else -> showGPSFailureSnackBar()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        setTitle(getString(R.string.content_text))

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener { saveReminderWithBackgroundPermission() }
        _viewModel.createGeofence.observe(this, { reminder ->
            geofenceManager.createGeofences(
                { requireContext().hasBackgroundPermissions() },
                _viewModel.setupGeofence(reminder)
            )
            _viewModel.goBack()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.clearData()
        dismissCurrentDialog()
    }

    private fun saveReminderWithBackgroundPermission() = with(requireActivity()) {
        when {
            hasBackgroundPermissions() -> runWithGPSEnabled { _viewModel.validateAndSaveReminder() }
            !hasFineLocationPermission() -> requestFineLocationPermission()
            shouldShowBackgroundLocationDialog() ->
                showPermissionRequiredDialog { requestBackgroundLocationPermission() }
            else -> requestBackgroundLocationPermission()
        }
    }

    private fun runWithGPSEnabled(onGPSEnabled: () -> Unit) {
        geofenceManager.verifyLocationSettings(
            locationSettings = locationServices,
            onSuccessListener = { onGPSEnabled() }
        ) { exception -> onGPSNotEnabled(exception) }
    }

    private fun onGPSNotEnabled(exception: Exception) = when (exception) {
        is ResolvableApiException ->
            gpsRequest.launch(IntentSenderRequest.Builder(exception.resolution).build())

        else -> showGPSFailureSnackBar()
    }

    private fun showGPSFailureSnackBar() = with(_viewModel) {
        showSnackBarInt.postValue(R.string.location_required_error)
    }

    private fun requestFineLocationPermission() {
        fineLocationRequest.launch(fineLocationPermission)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            backgroundLocationRequest.launch(backgroundPermission)
        else
            fineLocationRequest.launch(fineLocationPermission)
    }


    private fun showPermissionRequiredDialog(
        onAllowClicked: () -> Unit = { requireActivity().launchPermissionSettingsActivity() }
    ) {
        permissionDialog = requireActivity().showPermissionsRequiredDialog(
            message = R.string.permission_denied_explanation,
            autoDismiss = false,
        ) {
            onAllowClicked()
            permissionDialog.dismiss()
        }
    }

    private fun dismissCurrentDialog() {
        if (::permissionDialog.isInitialized && permissionDialog.isShowing)
            permissionDialog.tryDimiss()
    }
}
