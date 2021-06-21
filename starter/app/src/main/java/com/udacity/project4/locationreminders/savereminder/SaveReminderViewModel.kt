package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import java.util.*

class SaveReminderViewModel(
    val app: Application,
    val dataSource: ReminderDataSource
) : BaseViewModel(app) {

    private val TAG = SaveReminderViewModel::class.simpleName

    val createGeofence: SingleLiveEvent<ReminderDataItem> = SingleLiveEvent()

    private val selectedPOI = MutableLiveData<PointOfInterest?>()
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    fun getCurrentReminderItem() = ReminderDataItem(
        title = reminderTitle.value,
        description = reminderDescription.value,
        location = reminderSelectedLocationStr.value,
        latitude = latitude.value,
        longitude = longitude.value
    )

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
    }

    fun goBack() {
        navigationCommand.value = NavigationCommand.Back
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(
        reminderData: ReminderDataItem = getCurrentReminderItem()
    ) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
            createGeofence.postValue(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(
        reminderData: ReminderDataItem
    ): Boolean = when {
        reminderData.title.isNullOrEmpty() -> {
            showSnackBarInt.value = R.string.err_enter_title
            false
        }

        reminderData.location.isNullOrEmpty() -> {
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