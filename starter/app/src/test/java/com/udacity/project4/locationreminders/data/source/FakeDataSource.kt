package com.udacity.project4.locationreminders.data.source

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource(
    var reminders: MutableList<ReminderDTO>? = mutableListOf()
) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error(MULTIPLE_REMINDERS_GET_ERROR)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminders?.first { it.id == id }?.let { return Result.Success(it) }
        return Result.Error(SINGLE_REMINDER_GET_ERROR)
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    companion object {
        const val SINGLE_REMINDER_GET_ERROR = "Reminder not found"
        const val MULTIPLE_REMINDERS_GET_ERROR = "Reminders not found"
    }
}