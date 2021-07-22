package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dummy.DummyAuthViewModel
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.ReminderRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.source.RemindersLocalDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceManager
import com.udacity.project4.locationreminders.geofence.GeofenceManagerImpl
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.MainCoroutineRuleAndroidTests
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test
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

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        MockitoAnnotations.openMocks(this)
        stopKoin()//stop the original app koin

        appContext = getApplicationContext()
        dummyAuthViewModel = Mockito.spy(DummyAuthViewModel(getApplicationContext()))
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
                remindersListViewModel = Mockito.spy(RemindersListViewModel(get(), get()))
                remindersListViewModel
            }
        }
        //declare a new koin module
        startKoin { modules(listOf(myModule)) }

        reminderDataSource = get()
        //Get our real repository
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }

    @Test
    fun test() {

    }

//    TODO: add End to End testing to the app

}
