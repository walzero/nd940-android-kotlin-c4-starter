package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.findNavController
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dummy.DummyAuthViewModel
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.source.RemindersLocalDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.locationreminders.geofence.GeofenceManagerImpl
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.MainCoroutineRuleAndroidTests
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.spy
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest : AutoCloseKoinTest() {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRuleAndroidTests()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var dummyAuthViewModel: DummyAuthViewModel
    private lateinit var testNavHostController: TestNavHostController
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val googleHQLatLng by lazy { LatLng(37.419857, -122.078827) }


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
            single { spy(RemindersListViewModel(get(), get())) }
            single { spy(SaveReminderViewModel(get(), get())) }
        }

        //declare a new koin module
        startKoin { modules(listOf(myModule)) }

        reminderDataSource = get()
        remindersListViewModel = get()
        saveReminderViewModel = get()
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
    fun saveReminderFragment_setTitleAndDescription_showsTitleAndDescription() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)
        navController.navigate(R.id.to_save_reminder)
        assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

        runBlocking {
            val testTitle = "Test Title"
            onView(withId(R.id.reminderTitle)).perform(typeText(testTitle))
            onView(withId(R.id.reminderTitle)).check(matches(withText(testTitle)))

            val testDescription = "Test Description"
            onView(withId(R.id.reminderDescription)).perform(typeText(testDescription))
            onView(withId(R.id.reminderDescription)).check(matches(withText(testDescription)))
            activityScenario.close()
        }
    }

    @Test
    fun saveReminderFragment_withSetChosenLocationLatLng_showsLocation() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

        navController.navigate(R.id.to_save_reminder)
        assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

        navController.navigate(R.id.action_saveReminderFragment_to_selectLocationFragment)
        assertEquals(navController.currentDestination?.id, R.id.selectLocationFragment)

        runBlocking {
            saveReminderViewModel.setChosenLocation(googleHQLatLng)
            dataBindingIdlingResource.awaitUntilIdle()

            navController.popBackStack(R.id.selectLocationFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

            delay(3000)
            dataBindingIdlingResource.awaitUntilIdle()
            val expectedText = saveReminderViewModel.createSnippet(googleHQLatLng)
            onView(withId(R.id.selectedLocation)).check(matches(withText(expectedText)))
            activityScenario.close()
        }
    }

    @Test
    fun saveReminderFragment_withSetChosenLocationPOI_showsLocation() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

        navController.navigate(R.id.to_save_reminder)
        assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

        navController.navigate(R.id.action_saveReminderFragment_to_selectLocationFragment)
        assertEquals(navController.currentDestination?.id, R.id.selectLocationFragment)

        runBlocking {
            val googleHQId = "GOOGLE"
            val googleHQText = "Google HQ"
            val poi = PointOfInterest(googleHQLatLng, googleHQId, googleHQText)
            saveReminderViewModel.setChosenLocation(poi)
            dataBindingIdlingResource.awaitUntilIdle()

            navController.popBackStack(R.id.selectLocationFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

            delay(3000)
            dataBindingIdlingResource.awaitUntilIdle()
            onView(withId(R.id.selectedLocation)).check(matches(withText(googleHQText)))
            activityScenario.close()
        }
    }

    @Test
    fun saveReminderFragment_onSaveReminderWithPOI_savesReminder() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

        navController.navigate(R.id.to_save_reminder)
        assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

        runBlocking {
            val testTitle = "Test Title"
            onView(withId(R.id.reminderTitle)).perform(typeText(testTitle))
            onView(withId(R.id.reminderTitle)).check(matches(withText(testTitle)))

            val testDescription = "Test Description"
            onView(withId(R.id.reminderDescription)).perform(typeText(testDescription))
            onView(withId(R.id.reminderDescription)).check(matches(withText(testDescription)))

            navController.navigate(R.id.action_saveReminderFragment_to_selectLocationFragment)
            assertEquals(navController.currentDestination?.id, R.id.selectLocationFragment)

            val googleHQId = "GOOGLE"
            val googleHQText = "Google HQ"
            val poi = PointOfInterest(googleHQLatLng, googleHQId, googleHQText)
            saveReminderViewModel.setChosenLocation(poi)

            dataBindingIdlingResource.awaitUntilIdle()
            navController.popBackStack(R.id.selectLocationFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

            saveReminderViewModel.validateAndSaveReminder()
            dataBindingIdlingResource.awaitUntilIdle()

            navController.popBackStack(R.id.saveReminderFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

            delay(3000)
            dataBindingIdlingResource.awaitUntilIdle()
            assertEquals(1, remindersListViewModel.remindersList.value?.size)
            activityScenario.close()
        }
    }

    @Test
    fun saveReminderFragment_onSaveReminderWithLatLng_savesReminder() {
        withAuthenticationGranted()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val navController = dataBindingIdlingResource.activity.findNavController(R.id.nav_host_fragment)
        assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

        navController.navigate(R.id.to_save_reminder)
        assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

        runBlocking {
            val testTitle = "Test Title"
            onView(withId(R.id.reminderTitle)).perform(typeText(testTitle))
            onView(withId(R.id.reminderTitle)).check(matches(withText(testTitle)))

            val testDescription = "Test Description"
            onView(withId(R.id.reminderDescription)).perform(typeText(testDescription))
            onView(withId(R.id.reminderDescription)).check(matches(withText(testDescription)))

            navController.navigate(R.id.action_saveReminderFragment_to_selectLocationFragment)
            assertEquals(navController.currentDestination?.id, R.id.selectLocationFragment)

            saveReminderViewModel.setChosenLocation(googleHQLatLng)
            dataBindingIdlingResource.awaitUntilIdle()

            navController.popBackStack(R.id.selectLocationFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.saveReminderFragment)

            saveReminderViewModel.validateAndSaveReminder()
            dataBindingIdlingResource.awaitUntilIdle()

            navController.popBackStack(R.id.saveReminderFragment, true)
            assertEquals(navController.currentDestination?.id, R.id.reminderListFragment)

            delay(3000)
            dataBindingIdlingResource.awaitUntilIdle()
            assertEquals(1, remindersListViewModel.remindersList.value?.size)
            activityScenario.close()
        }
    }

    private fun withAuthenticationGranted() {
        dummyAuthViewModel.dummyAuthLiveData.postValue(AuthenticationViewModel.AuthenticationState.AUTHENTICATED)
        dummyAuthViewModel.expectedResultOnAuth = AuthenticationViewModel.AuthenticationState.AUTHENTICATED
    }
}