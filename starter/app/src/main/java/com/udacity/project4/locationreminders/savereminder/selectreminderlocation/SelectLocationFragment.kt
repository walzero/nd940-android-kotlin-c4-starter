package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private val TAG = SelectLocationFragment::class.java.simpleName
    private lateinit var map: GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding

    private var requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("DEBUG", "permission granted")
            } else {
                Log.i("DEBUG", "permission denied")
            }
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

        //todo ------------------------------------------------------------

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@SelectLocationFragment)

        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location

        //todo ------------------------------------------------------------

        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            val overlaySize = 100f

            val latitude = -23.54626445090568
            val longitude = -46.63790340398839

            val spLatlng = LatLng(latitude, longitude)
            val zoomLevel = 18f

//            setMapStyle()
            moveCameraToPosition(spLatlng, zoomLevel)
            addCurrentPositionMarker(spLatlng)
//            addGroundOverlay(createEmojiOverlay(spLatlng, overlaySize))
            setMapLongClickListener()
            setMapPOIClickListener()
        }

        map.enableMyLocation()
    }

    private fun GoogleMap.enableMyLocation() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (requiredPermissions.checkIfHasAllPermission()) {
            isMyLocationEnabled = true
        }
    }

    private fun GoogleMap.moveCameraToPosition(spLatlng: LatLng, zoomLevel: Float) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(spLatlng, zoomLevel))
    }

    private fun GoogleMap.addCurrentPositionMarker(spLatlng: LatLng) {
        addMarker(
            MarkerOptions()
                .position(spLatlng)
                .title(getString(R.string.current_location_pin))
                .snippet(createSnippet(spLatlng))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    private fun GoogleMap.setMapLongClickListener() {
        setOnMapLongClickListener { latLng ->
            addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(createSnippet(latLng))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
        }
    }

    private fun GoogleMap.setMapPOIClickListener() {
        setOnPoiClickListener { poi ->
            val poiMarker = addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            poiMarker.showInfoWindow()
        }
    }

//    private fun GoogleMap.setMapStyle() {
//        try {
//            val success = setMapStyle(
//                MapStyleOptions.loadRawResourceStyle(
//                    this@MapsActivity,
//                    R.raw.map_style
//                )
//            )
//
//            if (!success)
//                Log.e(TAG, getString(R.string.style_parsing_failed))
//        } catch (e: Resources.NotFoundException) {
//            Log.e(TAG, getString(R.string.style_not_found, e))
//        }
//    }

    private fun createSnippet(latLng: LatLng) = String.format(
        Locale.getDefault(),
        "Lat: %1$.5f, Long: %2$.5f",
        latLng.latitude,
        latLng.longitude
    )

    private fun Array<String>.checkIfHasAllPermission(): Boolean {
        forEach { permission ->
            if (getPermissionStatus(permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        return true
    }

    private fun getPermissionStatus(permission: String) =
        ActivityCompat.checkSelfPermission(requireContext(), permission)
}
