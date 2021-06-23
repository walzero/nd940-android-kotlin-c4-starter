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
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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

    @Captor
    private lateinit var loadingCaptor: ArgumentCaptor<Boolean>

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Before
    fun init() {
        remindersRepository = FakeTestRepository()
        remindersRepository.addReminders(*generateRandomReminders(4).toTypedArray())
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
    fun deleteAllGeofence_DeletesAllGeofences() {
        runBlocking {
            when (val reminders = remindersRepository.getReminders()) {
                is Result.Success -> assertFalse(reminders.data.isNullOrEmpty())
                is Result.Error -> fail()
            }

            remindersListViewModel.clearReminders()

            when (val reminders = remindersRepository.getReminders()) {
                is Result.Success -> assertTrue(reminders.data.isEmpty())
                is Result.Error -> fail()
            }
        }
    }
}