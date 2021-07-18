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
import kotlinx.coroutines.test.runBlockingTest
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
@Config(sdk = [Build.VERSION_CODES.Q])
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
        showSnackBarInt.observeForever(snackBarIntObserver)
        showLoading.observeForever(showLoadingObserver)
    }

    @Test
    fun goBack_PostsBack() {
        saveReminderViewModel.goBack()
        verify(navigationCommandObserver).onChanged(NavigationCommand.Back)
    }

    @Test
    fun validateAndSaveReminder_WithValidObjectCallsLoading() = runBlockingTest {
        setValuesForReminder()
        saveReminderViewModel.validateAndSaveReminder()
        verify(showLoadingObserver, times(2)).onChanged(showLoadingCaptor.capture())

        val capturedValues = showLoadingCaptor.allValues
        assertEquals(true, capturedValues[0])
        assertEquals(false, capturedValues[1])
    }

    @Test
    fun validateAndSaveReminder_WithValidObjectSavesReminder() = runBlockingTest {
        setValuesForReminder()
        saveReminderViewModel.validateAndSaveReminder()
        verify(createGeofenceObserver).onChanged(createGeofenceCaptor.capture())
        assertEquals(saveReminderViewModel.getCurrentReminderItem(), createGeofenceCaptor.value)
    }

    @Test
    fun validateAndSaveReminder_WithInvalidObjectDoesntCallSaveReminder() = runBlockingTest {
        setValuesForReminder(title = null, description = "")
        assert(saveReminderViewModel.getCurrentReminderItem()?.title.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
        verifyZeroInteractions(createGeofenceObserver)
    }

    @Test
    fun validateAndSaveReminder_WithInvalidTitleCallsShowSnackBarInt() = runBlockingTest {
        setValuesForReminder(title = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.title.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLocationCallsShowSnackBarInt() = runBlockingTest {
        setValuesForReminder(location = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.location.isNullOrBlank())
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLatitudeCallsShowSnackBarInt() = runBlockingTest {
        setValuesForReminder(latitude = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.latitude == null)
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun validateAndSaveReminder_WithInvalidLongitudeCallsShowSnackBarInt() = runBlockingTest {
        setValuesForReminder(longitude = null)
        assert(saveReminderViewModel.getCurrentReminderItem()?.longitude == null)
        saveReminderViewModel.validateAndSaveReminder()
        verify(snackBarIntObserver).onChanged(any())
    }

    @Test
    fun setChosenLocationLatLng_clearsSelectedPOI() {
        setValuesForReminder()
        assertNotNull(saveReminderViewModel.selectedPOI.value)
        saveReminderViewModel.setChosenLocation(LatLng(nextDouble(), nextDouble()))
        assertNull(saveReminderViewModel.selectedPOI.value)
    }

    @Test
    fun setChosenLocationLatLng_setsLocationAndLatitudeLongitude() {
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNull(saveReminderViewModel.latitude.value)
        assertNull(saveReminderViewModel.longitude.value)
        saveReminderViewModel.setChosenLocation(LatLng(nextDouble(), nextDouble()))
        assertNotNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNotNull(saveReminderViewModel.latitude.value)
        assertNotNull(saveReminderViewModel.longitude.value)
    }

    @Test
    fun setChosenLocationPOI_setsPOIValues() {
        assertNull(saveReminderViewModel.selectedPOI.value)
        assertNull(saveReminderViewModel.latitude.value)
        assertNull(saveReminderViewModel.longitude.value)
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        val idTest = UUID.randomUUID().toString()
        val nameTest = UUID.randomUUID().toString()
        saveReminderViewModel.setChosenLocation(PointOfInterest(LatLng(nextDouble(), nextDouble()), idTest, nameTest))
        assertNotNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNotNull(saveReminderViewModel.latitude.value)
        assertNotNull(saveReminderViewModel.longitude.value)
        assertNotNull(saveReminderViewModel.selectedPOI.value)
    }

    @Test
    fun clearChosenLocation_clearsLocationValues() {
        val idTest = UUID.randomUUID().toString()
        val nameTest = UUID.randomUUID().toString()
        saveReminderViewModel.setChosenLocation(PointOfInterest(LatLng(nextDouble(), nextDouble()), idTest, nameTest))
        assertNotNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNotNull(saveReminderViewModel.latitude.value)
        assertNotNull(saveReminderViewModel.longitude.value)
        assertNotNull(saveReminderViewModel.selectedPOI.value)

        saveReminderViewModel.clearChosenLocation()
        assertNull(saveReminderViewModel.selectedPOI.value)
        assertNull(saveReminderViewModel.latitude.value)
        assertNull(saveReminderViewModel.longitude.value)
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
    }

    @Test
    fun setupGeofence_withValidObjectReturnsGeofenceBuilderInstance() {
        setValuesForReminder()
        with(saveReminderViewModel) {
            getCurrentReminderItem()?.let { reminder ->
                val geofenceBuilder = setupGeofence(reminder)
                assertNotNull(geofenceBuilder)
            } ?: fail()
        }
    }

    @Test
    fun setupGeofence_withInvalidObjectReturnsNull() {
        val invalidReminderDataItem = ReminderDataItem(
            title = null,
            description = null,
            location = null,
            latitude = null,
            longitude = null
        )
        val geofenceBuilder = saveReminderViewModel.setupGeofence(invalidReminderDataItem)
        assertNull(geofenceBuilder)
    }

    @Test
    fun clearData_ClearsAllData() {
        setValuesForReminder()

        with(saveReminderViewModel) {
            assertNotNull(reminderTitle.value)
            assertNotNull(reminderDescription.value)
            assertNotNull(reminderSelectedLocationStr.value)
            assertNotNull(selectedPOI.value)
            assertNotNull(latitude.value)
            assertNotNull(longitude.value)

            clearData()

            assertNull(reminderTitle.value)
            assertNull(reminderDescription.value)
            assertNull(reminderSelectedLocationStr.value)
            assertNull(selectedPOI.value)
            assertNull(latitude.value)
            assertNull(longitude.value)
        }
    }

    private fun setValuesForReminder(
        title: String? = "Google",
        description: String? = "Google HQ Location",
        location: String? = UUID.randomUUID().toString(),
        latitude: Double? = 37.4220,
        longitude: Double? = -122.0840,
        poi: PointOfInterest? = PointOfInterest(
            LatLng(37.4220, -122.0840),
            "Google",
            "Google HQ Location"
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
