package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

interface ReminderRepository {
    /**
     * Get the reminders list from the local db
     * @return Result the holds a Success with all the reminders or an Error object with the error message
     */
    suspend fun getReminders(): Result<List<ReminderDTO>>

    /**
     * Insert a reminder in the db.
     * @param reminder the reminders to be inserted
     */
    suspend fun saveReminder(vararg reminder: ReminderDTO)

    /**
     * Get a reminder by its id
     * @param id to be used to get the reminder
     * @return Result the holds a Success object with the Reminder or an Error object with the error message
     */
    suspend fun getReminder(id: String): Result<ReminderDTO>

    /**
     * Deletes all the reminders in the db
     */
    suspend fun deleteAllReminders()
}