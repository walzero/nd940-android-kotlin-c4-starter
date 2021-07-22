package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
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

    //location stuff
    private lateinit var permissionDialog: MaterialDialog
    private val fineLocationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if(!result && canRequestFineLocationPermission()) {
            showPermissionRequiredDialog()
            return@registerForActivityResult
        }

        startPermissionsRequestForBackgroundLocation()
    }

    private val backgroundLocationRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            when (result) {
                true -> saveReminderWithBackgroundPermission()
                else -> startPermissionsRequestForBackgroundLocation()
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

    private fun saveReminderWithBackgroundPermission() {
        if (requireContext().hasBackgroundPermissions()) _viewModel.validateAndSaveReminder()
        else backgroundLocationRequest.launch(backgroundPermission)
    }

    private fun requestFineLocationPermission() =
        fineLocationRequest.launch(fineLocationPermission)

    private fun startPermissionsRequestForBackgroundLocation() {
        when {
            canRequestFineLocationPermission() -> requestFineLocationPermission()
            else -> showPermissionRequiredDialog()
        }
    }

    private fun showPermissionRequiredDialog() {
        permissionDialog = requireActivity().showPermissionsRequiredDialog(
            message = R.string.permission_denied_explanation,
            autoDismiss = false,
        ) {
            requireActivity().launchPermissionSettingsActivity()
            permissionDialog.dismiss()
        }
    }

    private fun dismissCurrentDialog() {
        if (::permissionDialog.isInitialized && permissionDialog.isShowing)
            permissionDialog.tryDimiss()
    }
}
