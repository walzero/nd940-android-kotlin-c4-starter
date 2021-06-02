package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import org.koin.android.ext.android.inject
import com.google.android.gms.location.*

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
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

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val reminderDataItem = ReminderDataItem(
                title = _viewModel.reminderTitle.value,
                description = _viewModel.reminderDescription.value,
                location = _viewModel.reminderSelectedLocationStr.value,
                latitude = _viewModel.latitude.value,
                longitude = _viewModel.longitude.value
            )

            addGeofence(reminderDataItem)
        }
    }

    private fun addGeofence(reminderDataItem: ReminderDataItem) {
        reminderDataItem.takeUnless {
            (it.latitude == null) or (it.longitude == null)
        }?.let {
            val reminderId = _viewModel.validateAndSaveReminder(reminderDataItem)
            createGeofence(reminderDataItem, reminderId)
        }
    }

    private fun createGeofence(reminderDataItem: ReminderDataItem, reminderId: Unit) {
        try {
            Geofence.Builder()
                .setRequestId(reminderId.toString())
                .setCircularRegion(
                    reminderDataItem.latitude!!,
                    reminderDataItem.longitude!!,
                    100f
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
