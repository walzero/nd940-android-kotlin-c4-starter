package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.SingleLiveEvent
import com.udacity.project4.utils.addSourceThenPost
import kotlinx.coroutines.launch
import java.util.*

class SaveReminderViewModel(
    app: Application,
    private val repository: ReminderRepository
) : BaseViewModel(app) {

    private val TAG = SaveReminderViewModel::class.simpleName

    val createGeofence: SingleLiveEvent<ReminderDataItem> = SingleLiveEvent()

    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    private var _currentReminderItem = ReminderDataItem()


    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun getCurrentReminderItem() = _currentReminderItem.apply {
        title = this@SaveReminderViewModel.reminderTitle.value
        description = this@SaveReminderViewModel.reminderDescription.value
        location = this@SaveReminderViewModel.reminderSelectedLocationStr.value
        latitude = this@SaveReminderViewModel.latitude.value
        longitude = this@SaveReminderViewModel.longitude.value
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun clearData() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
        _currentReminderItem = ReminderDataItem()
    }

    fun goBack() {
        navigationCommand.value = NavigationCommand.Back
    }

    /**
     * Validate the entered data then saves the reminder data to the repository
     */
    fun validateAndSaveReminder(
        reminderData: ReminderDataItem = getCurrentReminderItem() ?: ReminderDataItem()
    ) = viewModelScope.launch {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData).also { createGeofence.postValue(reminderData) }
        }
    }

    /**
     * Save the reminder to the repository
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        repository.saveReminder(
            ReminderDTO(
                title = reminderData.title,
                description = reminderData.description,
                location = reminderData.location,
                latitude = reminderData.latitude,
                longitude = reminderData.longitude,
                id = reminderData.id
            )
        )
        showLoading.value = false
        showToastInt.value = R.string.reminder_saved
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(
        reminderData: ReminderDataItem
    ): Boolean = when {
        reminderData.title.isNullOrBlank() -> {
            showSnackBarInt.value = R.string.err_enter_title
            false
        }

        reminderData.location.isNullOrBlank() -> {
            showSnackBarInt.value = R.string.err_select_location
            false
        }

        (reminderData.latitude == null) or (reminderData.longitude == null) -> {
            showSnackBarInt.value = R.string.geofence_unknown_error
            false
        }

        else -> true
    }

    fun setChosenLocation(latLng: LatLng) {
        selectedPOI.value = null
        reminderSelectedLocationStr.value = createSnippet(latLng)
        latitude.value = latLng.latitude
        longitude.value = latLng.longitude
    }

    fun setChosenLocation(poi: PointOfInterest) {
        selectedPOI.value = poi
        latitude.value = poi.latLng.latitude
        longitude.value = poi.latLng.longitude
        reminderSelectedLocationStr.value = poi.name ?: createSnippet(poi.latLng)
    }

    fun clearChosenLocation() {
        selectedPOI.value = null
        reminderSelectedLocationStr.value = null
        latitude.value = null
        longitude.value = null
    }

    fun createSnippet(latLng: LatLng) = String.format(
        Locale.getDefault(),
        "Lat: %1$.5f, Long: %2$.5f",
        latLng.latitude,
        latLng.longitude
    )

    fun setupGeofence(
        reminderDataItem: ReminderDataItem
    ): Geofence? = try {
        Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                100f
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    } catch (e: Exception) {
        Log.e(TAG, e.toString())
        null
    }
}