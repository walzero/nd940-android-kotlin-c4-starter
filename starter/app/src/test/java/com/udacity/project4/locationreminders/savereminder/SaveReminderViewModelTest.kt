package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.source.FakeTestRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import java.util.*
import kotlin.random.Random.Default.nextDouble


@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@MediumTest
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: FakeTestRepository

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Mock
    private lateinit var createGeofenceObserver: Observer<ReminderDataItem>

    @Mock
    private lateinit var navigationCommandObserver: Observer<NavigationCommand>

    @Mock
    private lateinit var currentReminderItemObserver: Observer<ReminderDataItem>

    @Mock
    private lateinit var showLoadingObserver: Observer<Boolean>

    @Mock
    private lateinit var snackBarIntObserver: Observer<Int>

    @Captor
    private lateinit var showLoadingCaptor: ArgumentCaptor<Boolean>

    @Captor
    private lateinit var createGeofenceCaptor: ArgumentCaptor<ReminderDataItem>

    @Captor
    private lateinit var navigationCommandCaptor: ArgumentCaptor<NavigationCommand>

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Before
    fun init() {
        remindersRepository = FakeTestRepository()
        saveReminderViewModel = SaveReminderViewModel(getApplicationContext(), remindersRepository)
        addObservers()
    }

    @After
    fun reset() {
        stopKoin()
    }

    private fun addObservers() = with(saveReminderViewModel) {
        createGeofence.observeForever(createGeofenceObserver)
        navigationCommand.observeForever(navigationCommandObserver)
        currentReminderItem.observeForever(currentReminderItemObserver)
        showSnackBarInt.observeForever(snackBarIntObserver)
        showLoading.observeForever(showLoadingObserver)
    }

    @Test
    fun goBack_PostsBack() {
        saveReminderViewModel.goBack()
        verify(navigationCommandObserver).onChanged(NavigationCommand.Back)
    }

    @Test
    fun validateAndSaveReminder_WithValidObjectCallsLoading() = runBlocking {
        setRandomValuesForReminder()
        saveReminderViewModel.validateAndSaveReminder()
        verify(showLoadingObserver, times(2)).onChanged(showLoadingCaptor.capture())

        val capturedValues = showLoadingCaptor.allValues
        assertEquals(true, capturedValues[0])
        assertEquals(false, capturedValues[1])
    }

    @Test
    fun validateAndSaveReminder_WithValidObjectSavesReminder() = runBlocking {
        setRandomValuesForReminder()
        saveReminderViewModel.validateAndSaveReminder()
        verify(createGeofenceObserver).onChanged(createGeofenceCaptor.capture())
        assertEquals(saveReminderViewModel.getCurrentReminderItem(), createGeofenceCaptor.value)
    }

    @Test
    fun validateAndSaveReminder_WithInvalidObjectDoesntCallSaveReminder() = runBlocking {
        setRandomValuesForReminder(title = null, description = "")
        assert(saveReminderViewModel.getCurrentReminderItem()?.title.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
        verifyZeroInteractions(createGeofenceObserver)
    }

    @Test
    fun validateAndSaveReminder_WithInvalidTitleCallsShowSnackBarInt() = runBlocking {
        setRandomValuesForReminder(title = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.title.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLocationCallsShowSnackBarInt() = runBlocking {
        setRandomValuesForReminder(location = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.location.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLatitudeCallsShowSnackBarInt() = runBlocking {
        setRandomValuesForReminder(latitude = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.latitude == null)
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLongitudeCallsShowSnackBarInt() = runBlocking {
        setRandomValuesForReminder(longitude = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.longitude == null)
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun setChosenLocation_clearsSelectedPOI() {
        setRandomValuesForReminder()
        assertNotNull(saveReminderViewModel.selectedPOI.value)
        saveReminderViewModel.setChosenLocation(LatLng(nextDouble(), nextDouble()))
        assertNull(saveReminderViewModel.selectedPOI.value)
    }

    @Test
    fun setChosenLocation_setsLocationAndLatitudeLongitude() {
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNull(saveReminderViewModel.latitude.value)
        assertNull(saveReminderViewModel.longitude.value)
        saveReminderViewModel.setChosenLocation(LatLng(nextDouble(), nextDouble()))
        assertNotNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNotNull(saveReminderViewModel.latitude.value)
        assertNotNull(saveReminderViewModel.longitude.value)
    }

    @Test
    fun clearData_ClearsAllData() {
        setRandomValuesForReminder()
        verify(currentReminderItemObserver, times(6)).onChanged(any())
        clearInvocations(currentReminderItemObserver)

        with(saveReminderViewModel) {
            Assert.assertNotNull(reminderTitle.value)
            Assert.assertNotNull(reminderDescription.value)
            Assert.assertNotNull(reminderSelectedLocationStr.value)
            Assert.assertNotNull(selectedPOI.value)
            Assert.assertNotNull(latitude.value)
            Assert.assertNotNull(longitude.value)

            clearData()

            verify(currentReminderItemObserver, times(6)).onChanged(any())
            Assert.assertNull(reminderTitle.value)
            Assert.assertNull(reminderDescription.value)
            Assert.assertNull(reminderSelectedLocationStr.value)
            Assert.assertNull(selectedPOI.value)
            Assert.assertNull(latitude.value)
            Assert.assertNull(longitude.value)
        }
    }

    private fun setRandomValuesForReminder(
        title: String? = UUID.randomUUID().toString(),
        description: String? = UUID.randomUUID().toString(),
        location: String? = UUID.randomUUID().toString(),
        latitude: Double? = nextDouble(),
        longitude: Double? = nextDouble(),
        poi: PointOfInterest? = PointOfInterest(
            LatLng(0.0, 0.0),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        )
    ) {
        saveReminderViewModel.let { vm ->
            vm.reminderTitle.value = title
            vm.reminderDescription.value = description
            vm.reminderSelectedLocationStr.value = location
            vm.latitude.value = latitude
            vm.longitude.value = longitude
            vm.selectedPOI.value = poi
        }
    }
}
