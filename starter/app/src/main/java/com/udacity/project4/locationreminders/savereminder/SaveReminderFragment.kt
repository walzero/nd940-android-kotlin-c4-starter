package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.base.BaseLocationFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseLocationFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private val geofenceManager: GeofenceManager by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private val TAG = SaveReminderFragment::class.simpleName

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

        binding.saveReminder.setOnClickListener { onSaveReminder() }
        _viewModel.createGeofence.observe(this, { reminder ->
            geofenceManager.createGeofences(::hasLocationPermissions, _viewModel.setupGeofence(reminder))
            _viewModel.goBack()
        })
    }

    private fun onSaveReminder() =
        runWithBackgroundPermission {  _viewModel.validateAndSaveReminder() }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.clearData()
    }
}
