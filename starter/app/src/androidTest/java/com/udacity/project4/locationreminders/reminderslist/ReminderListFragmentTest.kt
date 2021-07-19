package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.findNavController
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.authentication.AuthenticationViewModel.AuthenticationState.AUTHENTICATED
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dummy.DummyAuthViewModel
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.source.RemindersLocalDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.locationreminders.geofence.GeofenceManagerImpl
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.MainCoroutineRuleAndroidTests
import com.udacity.project4.util.generateRandomReminders
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRuleAndroidTests()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var dummyAuthViewModel: DummyAuthViewModel
    private lateinit var testNavHostController: TestNavHostController
    private lateinit var remindersListViewModel: RemindersListViewModel
    private val dataBindingIdlingResource = DataBindingIdlingResource()


    @Before
    fun init() {
        MockitoAnnotations.openMocks(this)
        stopKoin()//stop the original app koin

        appContext = getApplicationContext()
        dummyAuthViewModel = spy(DummyAuthViewModel(getApplicationContext()))
        testNavHostController = TestNavHostController(appContext)

        val myModule = module {
            single<Application> { getApplicationContext() }
            single { dummyAuthViewModel }
            single<AuthenticationViewModel> { dummyAuthViewModel }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
            single<ReminderDataSource> { RemindersLocalDataSource(get()) }
            single<ReminderRepository> { RemindersLocalRepository(get()) }
            single<GeofenceManager> { GeofenceManagerImpl(get()) }
            single { SaveReminderViewModel(get(), get()) }
            single {
                remindersListViewModel = spy(RemindersListViewModel(get(), get()))
                remindersListViewModel
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        reminderDataSource = get()
        //Get our real repository
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
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
    fun reminderListFragment_withoutItems_showsTheEmptyState() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.remindersRecyclerView)).check { view, noViewFoundException ->
            noViewFoundException?.let { throw it }
            assert(view is RecyclerView)
            assertEquals(0, (view as RecyclerView).adapter?.itemCount ?: -1)
        }
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun reminderListFragment_onRemindersDataChanged_updatesTheItems() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

        onView(withId(R.id.remindersRecyclerView)).check { view, noViewFoundException ->
            noViewFoundException?.let { throw it }
            assert(view is RecyclerView)
            assertEquals(0, (view as RecyclerView).adapter?.itemCount ?: -1)
        }

        runBlocking {
            withContext(Dispatchers.IO) {
                val randomReminders = generateRandomReminders(2)
                reminderDataSource.saveReminder(*randomReminders.toTypedArray())

                dataBindingIdlingResource.awaitUntilIdle()
                remindersListViewModel.loadReminders()
                dataBindingIdlingResource.awaitUntilIdle()
                withContext(Dispatchers.Main) {
                    verify(remindersListViewModel, times(2)).loadReminders()
                    onView(withId(R.id.remindersRecyclerView)).check { view, noViewFoundException ->
                        noViewFoundException?.let { throw it }
                        assert(view is RecyclerView)
                        assertEquals(2, (view as RecyclerView).adapter?.itemCount ?: -1)
                    }
                }
                activityScenario.close()
            }
        }
    }

    private fun withAuthenticationGranted() {
        dummyAuthViewModel.dummyAuthLiveData.postValue(AUTHENTICATED)
        dummyAuthViewModel.expectedResultOnAuth = AUTHENTICATED
    }
}