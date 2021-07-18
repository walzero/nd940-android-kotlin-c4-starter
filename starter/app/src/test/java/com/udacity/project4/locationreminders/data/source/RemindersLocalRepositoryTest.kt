package com.udacity.project4.locationreminders.data.source

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.source.FakeDataSource.Companion.SINGLE_REMINDER_GET_ERROR
import com.udacity.project4.locationreminders.data.source.FakeTestRepository.Companion.generateRandomReminders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var localDataSource: FakeDataSource

    private lateinit var remindersRepository: RemindersLocalRepository

    private val testItems by lazy { generateRandomReminders(3) }

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun createRepository() {
        localDataSource = FakeDataSource(testItems.toMutableList())
        // Get a reference to the class under test
        remindersRepository = RemindersLocalRepository(localDataSource)
    }

    @After
    fun reset() {
        stopKoin()
    }

    @Test
    fun localDataSource_hasInitialItems() = runBlockingTest {
        val result = remindersRepository.getReminders()
        assert(result is Success)
        assertEquals(testItems.count(), (result as Success).data.count())
    }

    @Test
    fun getReminders_getsAllReminders() = runBlockingTest {
        val reminders = remindersRepository.getReminders()
        assert(reminders is Success)
        assertEquals(
            testItems.sortedBy { it.id },
            (reminders as Success).data.sortedBy { it.id }
        )
    }

    @Test
    fun saveReminder_savesTheReminder() = runBlockingTest {
        val dummyReminder = generateRandomReminders(1)
            .first()
            .apply { id = testItems.count().inc().toString() }

        remindersRepository.saveReminder(dummyReminder)
        val result = remindersRepository.getReminder(dummyReminder.id)
        assert(result is Success)
        assertEquals(dummyReminder, (result as Success).data)
    }

    @Test
    fun getReminder_ReturnsReminderWithValidId() = runBlockingTest {
        testItems.forEachIndexed { index, reminder ->
            val result = remindersRepository.getReminder(reminder.id)
            assert(result is Success)
            assertEquals(testItems[index], (result as Success).data)
        }
    }

    @Test
    fun getReminder_ReturnsErrorWithInvalidId() = runBlockingTest {
        val id = testItems.joinToString { it.id }
        val result = remindersRepository.getReminder(id)
        assert(result is Error)
        assertEquals(SINGLE_REMINDER_GET_ERROR, (result as Error).message)
    }

    @Test
    fun deleteAllReminders() = runBlockingTest {
        remindersRepository.deleteAllReminders()
        val result = remindersRepository.getReminders()
        assert(result is Success)
        assertEquals(0, (result as Success).data.count())
    }
}