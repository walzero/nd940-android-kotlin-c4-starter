package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.authentication.AuthenticatedActivity
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dummy.DummyAuthViewModel
import com.udacity.project4.locationreminders.data.local.source.RemindersLocalDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.locationreminders.geofence.GeofenceManagerImpl
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.MainCoroutineRuleAndroidTests
import com.udacity.project4.util.generateRandomReminders
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest : AutoCloseKoinTest() {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRuleAndroidTests()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var appContext: Application
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var dummyAuthViewModel: DummyAuthViewModel
    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var repository: ReminderRepository

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        stopKoin()//stop the original app koin

        appContext = ApplicationProvider.getApplicationContext()
        dummyAuthViewModel = Mockito.spy(DummyAuthViewModel(ApplicationProvider.getApplicationContext()))

        val myModule = module {
            single<Application> { ApplicationProvider.getApplicationContext() }
            single { dummyAuthViewModel }
            single<AuthenticationViewModel> { dummyAuthViewModel }
            single { Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().context,
                RemindersDatabase::class.java
            ).allowMainThreadQueries().build().reminderDao() }
            single<ReminderDataSource> { RemindersLocalDataSource(get()) }
            single<ReminderRepository> { RemindersLocalRepository(get()) }
            single<GeofenceManager> { GeofenceManagerImpl(get()) }
            single { SaveReminderViewModel(get(), get()) }
            single {
                remindersListViewModel = Mockito.spy(RemindersListViewModel(get(), get()))
                remindersListViewModel
            }
        }
        //declare a new koin module
        startKoin { modules(listOf(myModule)) }

        reminderDataSource = get()
        repository = get()
        //Get our real repository
        runBlocking { reminderDataSource.deleteAllReminders() }

        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun remindersLocalRepository_onSaveReminder_savesTheReminders() = runBlocking {
        val reminders = generateRandomReminders(5)
        repository.saveReminder(reminders.first())
        dataBindingIdlingResource.awaitUntilIdle()

        val fetchSavedReminder = repository.getReminder(reminders.first().id)
        dataBindingIdlingResource.awaitUntilIdle()
        assert(fetchSavedReminder is Result.Success)
        val savedReminder = (fetchSavedReminder as Result.Success).data

        val firstReminder = reminders.first()
        assertEquals(firstReminder.id, savedReminder.id)
        assertEquals(firstReminder.title, savedReminder.title)
        assertEquals(firstReminder.location, savedReminder.location)
        assertEquals(firstReminder.longitude, savedReminder.longitude)
        assertEquals(firstReminder.latitude, savedReminder.latitude)
        assertEquals(firstReminder.description, savedReminder.description)

        repository.saveReminder(*reminders.minus(reminders.first()).toTypedArray())
        dataBindingIdlingResource.awaitUntilIdle()

        val fetchSavedReminders = repository.getReminders()
        dataBindingIdlingResource.awaitUntilIdle()
        assert(fetchSavedReminders is Result.Success)
        val savedReminders = (fetchSavedReminders as Result.Success).data

        assertEquals(reminders.count(), savedReminders.count())
        reminders.forEach { rem -> assert(savedReminders.any { it.id == rem.id }) }
    }

    @Test
    fun remindersLocalRepository_getReminders_getsAllReminders() = runBlocking {
        val reminders = generateRandomReminders(3)
        repository.saveReminder(*reminders.toTypedArray())
        dataBindingIdlingResource.awaitUntilIdle()

        val fetchSavedReminders = repository.getReminders()
        dataBindingIdlingResource.awaitUntilIdle()
        assert(fetchSavedReminders is Result.Success)
        val savedReminders = (fetchSavedReminders as Result.Success).data

        assertEquals(reminders.count(), savedReminders.count())
        reminders.forEach { rem ->
            val matchedReminder = savedReminders.first { it.id == rem.id }
            assertEquals(rem.title, matchedReminder.title)
            assertEquals(rem.location, matchedReminder.location)
            assertEquals(rem.longitude, matchedReminder.longitude)
            assertEquals(rem.latitude, matchedReminder.latitude)
            assertEquals(rem.description, matchedReminder.description)
        }
    }

    @Test
    fun remindersLocalRepository_getReminder_getsReminderWithValidIds() = runBlocking {
        val reminders = generateRandomReminders(3)
        repository.saveReminder(*reminders.toTypedArray())
        dataBindingIdlingResource.awaitUntilIdle()

        reminders.forEach {
            val fetchSavedReminder = repository.getReminder(it.id)
            dataBindingIdlingResource.awaitUntilIdle()

            assert(fetchSavedReminder is Result.Success)
            val savedReminder = (fetchSavedReminder as Result.Success).data

            assertEquals(it.id, savedReminder.id)
            assertEquals(it.title, savedReminder.title)
            assertEquals(it.location, savedReminder.location)
            assertEquals(it.longitude, savedReminder.longitude)
            assertEquals(it.latitude, savedReminder.latitude)
            assertEquals(it.description, savedReminder.description)
        }
    }

    @Test
    fun remindersLocalRepository_getReminder_returnsErrorWithInvalidId() = runBlocking {
        val reminders = generateRandomReminders(4)
        val notInsertedReminder = reminders.last()
        //save all except the last one
        repository.saveReminder(*reminders.minus(notInsertedReminder).toTypedArray())
        dataBindingIdlingResource.awaitUntilIdle()

        val fetchReminder = repository.getReminder(notInsertedReminder.id)
        dataBindingIdlingResource.awaitUntilIdle()

        assert(fetchReminder is Result.Error)
        val failedReminder = (fetchReminder as Result.Error)

        assertEquals("Reminder not found!", failedReminder.message)
    }

    @Test
    fun remindersLocalRepository_deleteAllReminders_deletesAllReminders() = runBlocking {
        val reminders = generateRandomReminders(6)
        repository.saveReminder(*reminders.toTypedArray())
        dataBindingIdlingResource.awaitUntilIdle()

        repository.deleteAllReminders()
        dataBindingIdlingResource.awaitUntilIdle()

        val fetchSavedReminders = repository.getReminders()
        dataBindingIdlingResource.awaitUntilIdle()
        assert(fetchSavedReminders is Result.Success)
        val savedReminders = (fetchSavedReminders as Result.Success).data

        assertEquals(0, savedReminders.count())
    }

    private fun withAuthenticationGranted() {
        dummyAuthViewModel.dummyAuthLiveData.postValue(AuthenticationViewModel.AuthenticationState.AUTHENTICATED)
        dummyAuthViewModel.expectedResultOnAuth = AuthenticationViewModel.AuthenticationState.AUTHENTICATED
    }
}