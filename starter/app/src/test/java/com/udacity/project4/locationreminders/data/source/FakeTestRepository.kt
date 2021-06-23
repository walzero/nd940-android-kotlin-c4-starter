package com.udacity.project4.locationreminders.data.source

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.source.FakeDataSource.Companion.SINGLE_REMINDER_GET_ERROR
import java.util.*
import kotlin.random.Random.Default.nextDouble

class FakeTestRepository : ReminderDataSource {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error(TEST_EXCEPTION)
        }
        return Result.Success(remindersServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error(TEST_EXCEPTION)
        }
        remindersServiceData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error(SINGLE_REMINDER_GET_ERROR)
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.clear()
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
    }

    companion object {
        const val TEST_EXCEPTION = "Test exception"

        fun generateRandomReminders(amountToGenerate: Int = 1): List<ReminderDTO> {
            val amount = amountToGenerate.takeIf { it > 0 } ?: 1
            val list = mutableListOf<ReminderDTO>()

            for (i in 1..amount) {
                list.add(
                    ReminderDTO(
                        id = i.toString(),
                        title = UUID.randomUUID().toString(),
                        description = UUID.randomUUID().toString(),
                        location = UUID.randomUUID().toString(),
                        longitude = nextDouble(),
                        latitude = nextDouble()
                    )
                )
            }

            return list
        }
    }
}