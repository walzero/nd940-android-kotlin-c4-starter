package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.authentication.AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED
import com.udacity.project4.authentication.LoginActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dummy.DummyAuthViewModel
import com.udacity.project4.locationreminders.data.dummy.DummyAuthenticatedActivity
import com.udacity.project4.locationreminders.data.dummy.DummyAuthenticationActivity
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.source.RemindersLocalDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.locationreminders.geofence.GeofenceManagerImpl
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.*
import com.udacity.project4.utils.EspressoIdlingResource
import junit.framework.Assert.assertFalse
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
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest : AutoCloseKoinTest() {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRuleAndroidTests()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var reminderRepository: ReminderRepository
    private lateinit var appContext: Application
    private lateinit var dummyAuthViewModel: DummyAuthViewModel
    private lateinit var testNavHostController: TestNavHostController
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
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
            single { RemindersListViewModel(get(), get()) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        reminderRepository = get()
        //Get our real repository
        runBlocking {
            reminderRepository.deleteAllReminders()
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
    fun authenticatedActivity_withoutUser_opensLoginActivity() {
        Intents.init()
        val activityScenario = ActivityScenario.launch(DummyAuthenticatedActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        intended(hasComponent(LoginActivity::class.java.name))
        Intents.release()
        activityScenario.close()
    }

    @Test
    fun loginActivity_withoutUser_opensAuthenticationFlow() {
        dummyAuthViewModel.expectedResultOnAuth = UNAUTHENTICATED
        Intents.init()
        val activityScenario = ActivityScenario.launch(LoginActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.loginButton)).perform(click())
        verify(dummyAuthViewModel).createFirebaseAuthIntent()
        intended(hasComponent(DummyAuthenticationActivity::class.java.name))
        Intents.release()
        activityScenario.close()
    }

    @Test
    fun authenticationActivity_withoutSuccess_closesLogin() {
        dummyAuthViewModel.expectedResultOnAuth = UNAUTHENTICATED
        Intents.init()
        val activityScenario = ActivityScenario.launch(LoginActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        assertFalse(dataBindingIdlingResource.activity.isDestroyed)
        onView(withId(R.id.loginButton)).perform(click())
        verify(dummyAuthViewModel).createFirebaseAuthIntent()
        intended(hasComponent(DummyAuthenticationActivity::class.java.name))
        assert(dataBindingIdlingResource.activity.isDestroyed)
        Intents.release()
        activityScenario.close()
    }

    @Test
    fun reminderListFragment_addLocationReminderClick_opensSaveReminderFragment() {
        putLocationPermissions()
        val fragmentScenario = launchFragmentScenario<ReminderListFragment>(initialState = Lifecycle.State.STARTED)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)
        waitForInSeconds(1)
        val fragment = dataBindingIdlingResource.fragment
        testNavHostController.apply {
            setLifecycleOwner(fragment.requireActivity())
            setOnBackPressedDispatcher(fragment.requireActivity().onBackPressedDispatcher)
            setGraph(R.navigation.nav_graph)
            navigate(R.id.reminderListFragment)
        }
        fragment.setNavController(testNavHostController)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)
        onView(withId(R.id.addReminderFAB)).perform(click())
        assert(testNavHostController.currentDestination?.id == R.id.saveReminderFragment)
    }

    @Test
    fun saveReminderFragment_selectLocationClick_opensSelectLocationFragment() {
        putLocationPermissions()
        val fragmentScenario = launchFragmentScenario<SaveReminderFragment>(initialState = Lifecycle.State.STARTED)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)
        waitForInSeconds(1)
        val fragment = dataBindingIdlingResource.fragment
        testNavHostController.apply {
            setLifecycleOwner(fragment.requireActivity())
            setOnBackPressedDispatcher(fragment.requireActivity().onBackPressedDispatcher)
            setGraph(R.navigation.nav_graph)
            navigate(R.id.reminderListFragment)
            navigate(R.id.to_save_reminder)
        }
        fragment.setNavController(testNavHostController)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)
        onView(withId(R.id.selectLocation)).perform(click())
        assert(testNavHostController.currentDestination?.id == R.id.selectLocationFragment)
    }

}