package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseLocationFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseLocationFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private val TAG = SelectLocationFragment::class.java.simpleName

    private val mapFragment by lazy {
        childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
    }

    private lateinit var map: GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var snackbar: Snackbar
    private var selectedLocation: Boolean = false

    private var lastKnownLocation: Location? = null
    private var positionMarker: Marker? = null
        set(value) {
            field?.remove()
            field = value
            field?.position?.showConfirmationSnackbar()
        }

    override fun onLocationPermissionsGranted() {
        if(::binding.isInitialized)
            binding.map.visibility = View.VISIBLE
        mapFragment.getMapAsync(this@SelectLocationFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        setTitle(getString(R.string.select_location))

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            GoogleMap.MAP_TYPE_NORMAL.changeMapType()
            true
        }
        R.id.hybrid_map -> {
            GoogleMap.MAP_TYPE_HYBRID.changeMapType()
            true
        }
        R.id.satellite_map -> {
            GoogleMap.MAP_TYPE_SATELLITE.changeMapType()
            true
        }
        R.id.terrain_map -> {
            GoogleMap.MAP_TYPE_TERRAIN.changeMapType()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            val overlaySize = 100f
            val zoomLevel = 18f

            setMapStyle()
            initializeCurrentLocation()
//            addGroundOverlay(createEmojiOverlay(spLatlng, overlaySize))
            setMapLongClickListener()
            setMapPOIClickListener()
        }

        map.enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.initializeCurrentLocation() {
        if (hasPermissions()) {
            val locationTask = _locationServices.lastLocation

            locationTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    lastKnownLocation = task.result
                    lastKnownLocation?.run {
                        val zoomLevel = 18f
                        val latLng = LatLng(latitude, longitude)
                        moveCameraToPosition(latLng, zoomLevel)
                        addCurrentPositionMarker(latLng)
                    }
                }
            }
        } else {
            Log.i("DEBUG", "permissions denied")
        }
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.enableMyLocation() {
        if (hasPermissions()) {
            isMyLocationEnabled = true
        }
    }

    private fun GoogleMap.moveCameraToPosition(spLatlng: LatLng, zoomLevel: Float) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(spLatlng, zoomLevel))
    }

    private fun GoogleMap.addCurrentPositionMarker(latlng: LatLng) {
        positionMarker = addMarker(
            MarkerOptions()
                .position(latlng)
                .title(getString(R.string.current_location_pin))
                .snippet(_viewModel.createSnippet(latlng))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        positionMarker?.showInfoWindow()
        _viewModel.setChosenLocation(latlng)
    }

    private fun GoogleMap.setMapLongClickListener() {
        setOnMapLongClickListener { latLng ->
            positionMarker = addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(_viewModel.createSnippet(latLng))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )

            positionMarker?.showInfoWindow()
            _viewModel.setChosenLocation(latLng)
        }
    }

    private fun GoogleMap.setMapPOIClickListener() {
        setOnPoiClickListener { poi ->
            positionMarker = addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .snippet(_viewModel.createSnippet(poi.latLng))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )

            positionMarker?.showInfoWindow()
            _viewModel.setChosenLocation(poi)
        }
    }

    private fun GoogleMap.setMapStyle() {
        try {
            val success = setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success)
                Log.e(TAG, getString(R.string.style_parsing_failed))
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, getString(R.string.style_not_found, e))
        }
    }

    private fun LatLng.showConfirmationSnackbar() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.DESTROYED)) {
            dismissSnackbar()

            snackbar = binding.root.showConfirmationSnackbar(
                text = _viewModel.createSnippet(this),
                actionText = getString(R.string.confirm)
            ) { sb ->
                selectedLocation = true
                onSelectPOI(sb)
            }
        }
    }

    private fun LatLng.onSelectPOI(sb: Snackbar) {
        sb.dismiss()
        requireContext().applicationContext.showShortToast(_viewModel.createSnippet(this))
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onDetach() {
        super.onDetach()

        if (!selectedLocation)
            _viewModel.clearChosenLocation()

        dismissSnackbar()
    }

    private fun dismissSnackbar() {
        if (::snackbar.isInitialized) snackbar.dismiss()
    }

    private fun Int.changeMapType(): Int {
        if (::map.isInitialized) {
            map.mapType = this
        }

        return this
    }
}
