package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.wrapEspressoIdlingResource
import kotlinx.coroutines.*

/**
 * Concrete implementation of a data source as a db.
 *
 * The repository is implemented so that you can focus on only testing it.
 *
 * @param remindersLocalDataSource the local datasource that for operations
 * @param ioDispatcher a coroutine dispatcher to offload the blocking IO tasks
 */
class RemindersLocalRepository(
    private val remindersLocalDataSource: ReminderDataSource
) : ReminderRepository {

    /**
     * Get the reminders list from the local db
     * @return Result the holds a Success with all the reminders or an Error object with the error message
     */
    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        wrapEspressoIdlingResource {
            return try {
                remindersLocalDataSource.getReminders()
            } catch (ex: Exception) {
                Result.Error(ex.localizedMessage)
            }
        }

    /**
     * Insert a reminder in the db.
     * @param reminder the reminders to be inserted
     */
    override suspend fun saveReminder(vararg reminder: ReminderDTO) =
        wrapEspressoIdlingResource {
            remindersLocalDataSource.saveReminder(*reminder)
        }

    /**
     * Get a reminder by its id
     * @param id to be used to get the reminder
     * @return Result the holds a Success object with the Reminder or an Error object with the error message
     */
    override suspend fun getReminder(id: String): Result<ReminderDTO> =
        wrapEspressoIdlingResource {
            return try {
                remindersLocalDataSource.getReminder(id)
            } catch (e: Exception) {
                Result.Error(e.localizedMessage)
            }
        }

    /**
     * Deletes all the reminders in the db
     */
    override suspend fun deleteAllReminders() {
        wrapEspressoIdlingResource {
            remindersLocalDataSource.deleteAllReminders()
        }
    }
}
