package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import kotlinx.coroutines.launch

class RemindersListViewModel(
    app: Application,
    private val repository: ReminderRepository
) : BaseViewModel(app) {

    // list that holds the reminder data to be displayed on the UI
    private val _remindersList = MutableLiveData<List<ReminderDataItem>>()
    val remindersList: LiveData<List<ReminderDataItem>>
        get() = _remindersList

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            val result = repository.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<List<ReminderDTO>> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll(result.data.map { reminder ->
                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    _remindersList.postValue(dataList)
                }
                is Result.Error ->
                    showSnackBar.postValue(result.message)
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.postValue(remindersList.value.isNullOrEmpty())
    }

    fun clearReminders() {
        viewModelScope.launch {
            repository.deleteAllReminders()
        }
    }
}