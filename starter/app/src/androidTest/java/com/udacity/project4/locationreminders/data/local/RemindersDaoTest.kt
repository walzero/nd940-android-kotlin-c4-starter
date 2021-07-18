package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.util.generateRandomReminders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_savesReminders() = runBlockingTest {
        with(database.reminderDao()) {
            assertEquals(0, getReminders().count())
            val reminders = generateRandomReminders(3)
            saveReminder(*reminders.toTypedArray())
            val savedReminders = getReminders()
            assertEquals(reminders.count(), savedReminders.count())
            assertEquals(reminders, savedReminders)
        }
    }

    @Test
    fun getReminders_getsAllReminders() = runBlockingTest {
        with(database.reminderDao()) {
            val reminders1 = generateRandomReminders(2)
            saveReminder(*reminders1.toTypedArray())

            val reminders2 = generateRandomReminders(3).apply {
                forEach { it.id += reminders1.count() }
            }
            saveReminder(*reminders2.toTypedArray())

            assertThat(reminders1.count() + reminders2.count(), `is`(this.getReminders().count()))
            val originalList = reminders1.plus(reminders2)
            getReminders().forEachIndexed { index, reminder ->
                assertEquals(reminder, originalList[index])
            }
        }
    }

    @Test
    fun getReminderById_getsSpecificReminder() = runBlockingTest {
        with(database.reminderDao()) {
            val dummyReminder = generateRandomReminders(1)
            saveReminder(*dummyReminder.toTypedArray())
            val reminder = getReminderById(dummyReminder[0].id)
            assertEquals(dummyReminder[0], reminder)
        }
    }

    @Test
    fun deleteAllReminders_DeletesAllReminders() = runBlockingTest {
        with(database.reminderDao()) {
            val itemCount = 2
            val reminders = generateRandomReminders(itemCount)
            saveReminder(*reminders.toTypedArray())
            assertThat(itemCount, `is`(getReminders().count()))
            deleteAllReminders()
            assertThat(0, `is`(getReminders().count()))
        }
    }
}