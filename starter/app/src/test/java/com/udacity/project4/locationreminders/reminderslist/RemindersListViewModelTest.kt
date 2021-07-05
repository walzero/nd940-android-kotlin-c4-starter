package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.source.FakeTestRepository
import com.udacity.project4.locationreminders.data.source.FakeTestRepository.Companion.TEST_EXCEPTION
import com.udacity.project4.locationreminders.data.source.FakeTestRepository.Companion.generateRandomReminders
import junit.framework.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@MediumTest
class RemindersListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: FakeTestRepository
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Mock
    private lateinit var remindersListObserver: Observer<List<ReminderDataItem>>

    @Mock
    private lateinit var loadingObserver: Observer<Boolean>

    @Mock
    private lateinit var snackbarObserver: Observer<String>

    @Captor
    private lateinit var loadingCaptor: ArgumentCaptor<Boolean>

    @Captor
    private lateinit var remindersCaptor: ArgumentCaptor<List<ReminderDataItem>>

    @Captor
    private lateinit var snackbarCaptor: ArgumentCaptor<String>

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Before
    fun init() {
        remindersRepository = FakeTestRepository()
        remindersListViewModel = RemindersListViewModel(getApplicationContext(), remindersRepository)
        addObservers()
    }

    @After
    fun reset() {
        stopKoin()
    }

    private fun addObservers() {
        remindersListViewModel.remindersList.observeForever(remindersListObserver)
        remindersListViewModel.showLoading.observeForever(loadingObserver)
        remindersListViewModel.showSnackBar.observeForever(snackbarObserver)
    }

    @Test
    fun initViewModelTest_remindersListHasObserver() {
        assert(remindersListViewModel.remindersList.hasActiveObservers())
    }

    @Test
    fun loadReminders_setsLoadingThenHidesLoading() {
        remindersListViewModel.loadReminders()

        verify(loadingObserver, times(2)).onChanged(loadingCaptor.capture())
        val results = loadingCaptor.allValues
        assert(results[0])
        assertFalse(results[1])
    }

    @Test
    fun loadRemninders_onSuccessPostsValue() = runBlocking {
        val randomReminders = generateRandomReminders(3)
        remindersRepository.addReminders(*randomReminders.toTypedArray())

        remindersListViewModel.loadReminders()

        verify(remindersListObserver).onChanged(remindersCaptor.capture())
        val results = remindersCaptor.value

        assertEquals(randomReminders.size, results.size)
        randomReminders.forEachIndexed { index, reminder ->
            assertEquals(reminder.id, results[index].id)
            assertEquals(reminder.title, results[index].title)
            assertEquals(reminder.description, results[index].description)
            assertEquals(reminder.latitude, results[index].latitude)
            assertEquals(reminder.longitude, results[index].longitude)
            assertEquals(reminder.location, results[index].location)

        }
    }

    @Test
    fun loadRemninders_onFailurePostsError() {
        remindersRepository.setReturnError(true)
        remindersListViewModel.loadReminders()
        verifyNoMoreInteractions(remindersListObserver)
        verify(snackbarObserver).onChanged(snackbarCaptor.capture())
        val message = snackbarCaptor.value

        assertEquals(TEST_EXCEPTION, message)
    }

    @Test
    fun deleteAllGeofence_DeletesAllGeofences() {
        remindersRepository.addReminders(*generateRandomReminders(4).toTypedArray())

        runBlocking {
            when (val reminders = remindersRepository.getReminders()) {
                is Result.Success -> assertFalse(reminders.data.isNullOrEmpty())
                is Result.Error -> fail()
            }

            remindersRepository.addReminders(*generateRandomReminders(4).toTypedArray())
            remindersListViewModel.clearReminders()

            when (val reminders = remindersRepository.getReminders()) {
                is Result.Success -> assertTrue(reminders.data.isEmpty())
                is Result.Error -> fail()
            }
        }
    }
}