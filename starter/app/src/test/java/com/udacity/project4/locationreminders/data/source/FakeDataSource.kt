package com.udacity.project4.locationreminders.data.source

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class FakeDataSource(
    var reminders: MutableList<ReminderDTO> = mutableListOf()
) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success(ArrayList(reminders))
    }

    override suspend fun saveReminder(vararg reminder: ReminderDTO) {
        reminders.addAll(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminders.firstOrNull { it.id == id }
            ?.let { return Result.Success(it) }
            ?: return Result.Error(SINGLE_REMINDER_GET_ERROR)
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    companion object {
        const val SINGLE_REMINDER_GET_ERROR = "Reminder not found"
    }
}