package com.application.bikestreets

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Mode
import com.application.bikestreets.api.modals.Mode.Companion.getMode
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.bottomsheet.BottomSheetClickListener
import com.application.bikestreets.constants.MapLayerConstants.SELECTED_ROUTE_MAP_LAYER
import com.application.bikestreets.constants.PreferenceConstants.KEEP_SCREEN_ON_PREFERENCE_KEY
import com.application.bikestreets.constants.PreferenceConstants.MAP_TYPE_PREFERENCE_KEY
import com.application.bikestreets.databinding.ActivityMainBinding
import com.application.bikestreets.terms.TermsOfUse
import com.application.bikestreets.utils.PERMISSIONS_REQUEST_LOCATION
import com.application.bikestreets.utils.addLayerBasedOnMapType
import com.application.bikestreets.utils.convertToMapboxGeometry
import com.application.bikestreets.utils.getColorHexString
import com.application.bikestreets.utils.getDefaultPackageName
import com.application.bikestreets.utils.hideCurrentRouteLayer
import com.application.bikestreets.utils.moveCamera
import com.application.bikestreets.utils.requestLocationPermission
import com.application.bikestreets.utils.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    BottomSheetClickListener {
    private lateinit var mapView: MapView

    private lateinit var locationEngine: LocationEngine
    private lateinit var location: Point

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var sharedPreferences: SharedPreferences


    private lateinit var binding: ActivityMainBinding
    private val vm: MainVM by viewModels()
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        mapView = binding.mapView
        setupMapboxMap()
    }

    private fun setScreenModeFromPreferences() {
        sharedPreferences = getSharedPreferences(getDefaultPackageName(this), MODE_PRIVATE)

        val keepScreenOnPreference =
            sharedPreferences.getBoolean(KEEP_SCREEN_ON_PREFERENCE_KEY, true)

        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (keepScreenOnPreference) {
            window.addFlags(keepScreenOnFlag)
        } else {
            window.clearFlags(keepScreenOnFlag)
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun launchTermsOfUse() {
        val intent = Intent(this, TermsOfUse::class.java).apply {}
        // start Terms Of Use activity regardless of whether or not they have any to sign. Don't
        // worry: it will bail right away if it decides that the user is up-to-date
        startActivity(intent)
    }


    private fun setupMapboxMap() {
        // Attribution
        mapView.logo.updateSettings {
            position = Gravity.TOP
        }
        mapView.attribution.updateSettings {
            position = Gravity.TOP
        }

        // Hide Scalebar
        mapView.scalebar.updateSettings { enabled = false }

        // Load Style
        mapView.getMapboxMap().also { mapboxMap ->
            vm.loadMapboxStyle(mapboxMap, this@MainActivity)
            loadLocation()
        }

        // Load Map Markers
        // TODO: do this in a different thread so UI is not blocked
        mapMarkersManager = MapMarkersManager(mapView)
    }

    private fun loadLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            mapView.location.updateSettings {
                enabled = true
            }

            mapView.location.addOnIndicatorPositionChangedListener(object :
                OnIndicatorPositionChangedListener {
                override fun onIndicatorPositionChanged(point: Point) {
                    location = point
                    moveCamera(map = mapView.getMapboxMap(), location = location)

                    mapView.location.removeOnIndicatorPositionChangedListener(this)
                }
            })
        }
    }


    // Once a search has kicked off, given the response API, we use that route to draw a polyline
    private fun displayRouteOnMap(routes: List<Route>?): List<Route>? {

        val mapStyle = mapView.getMapboxMap().getStyle()

        val selectedRouteGeometry: MutableList<Feature> = mutableListOf()

        routes?.forEach {
            val legs = it.legs
            val steps = legs.flatMap { leg -> leg.steps }
            steps.forEach { step ->
                if (getMode(step.mode) == Mode.PUSHING_BIKE) {

                    val mapBoxGeometry = convertToMapboxGeometry(step.geometry)
                    val properties = JsonObject()
                    properties.addProperty(
                        "stroke",
                        getColorHexString(this, R.color.sidewalk_segment)
                    )

                    selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
                } else {
                    val mapBoxGeometry = convertToMapboxGeometry(step.geometry)
                    val properties = JsonObject()
                    properties.addProperty(
                        "stroke",
                        getColorHexString(this, R.color.vamos_light_blue)
                    )

                    selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
                }
            }
        }


        val pushingFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(selectedRouteGeometry)


        /** Once a layer is added, we cannot delete and re-render it,
         *  instead we keep it hidden or override the route segment as needed
         *
         *  This should be done with a Polyline Annotation, but there currently isn't multicolored
         *  line support unless using a gradient
         */
        val layerSource =
            mapView.getMapboxMap().getStyle()?.getSourceAs<GeoJsonSource>(SELECTED_ROUTE_MAP_LAYER)

        if (layerSource == null) {
            mapStyle?.addSource(
                GeoJsonSource.Builder(SELECTED_ROUTE_MAP_LAYER)
                    .featureCollection(pushingFeatureCollection).build()
            )

            // Add layer above rendered routes
            mapStyle?.let { addLayerBasedOnMapType(this, it, SELECTED_ROUTE_MAP_LAYER) }

        } else {
            layerSource.featureCollection(pushingFeatureCollection)
        }

        return routes
    }

    /**
     * Native back button will do different actions based on what is open
     * 1st - clear out search and collapse bottomsheet
     * 2nd - if bottom sheet is collapsed, remove the currently shown route
     * 3rd - Close app if none of the above
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            // Clear search and collapse
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            vm.clearText()
        } else if (mapMarkersManager.hasMarkers) {
            hideCurrentRouteLayer(mapView.getMapboxMap())
            mapMarkersManager.clearMarkers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                setScreenModeFromPreferences()
            }

            MAP_TYPE_PREFERENCE_KEY -> {
                // call this function, only to update the map style
                vm.loadMapboxStyle(mapView.getMapboxMap(), context = this)
            }

            else -> {
                Log.e(javaClass.simpleName, "No preference action for key: $key")
            }
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, handle location access
                loadLocation()
                vm.enableFollowRiderButton()
            } else {
                showToast(this, getString(R.string.no_location_access))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSettingsButtonClicked() {

        // Set the callback in the fragment
        val aboutFragment = AboutFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment_container, aboutFragment).addToBackStack("null")
            .commit()
    }

    override fun onLocationButtonClicked() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            moveCamera(map = mapView.getMapboxMap(), location = location)
        } else {
            requestLocationPermission(this)
        }
    }

    override fun showRoutes(startLocation: Location?, endLocation: Location) {
        if (isPossibleRoute(startLocation)) {
            val startCoordinates = startLocation?.coordinate ?: location

            MainScope().launch(Dispatchers.Main) {
                try {
                    val routingService = RoutingService()
                    val routingDirections = routingService.getRoutingDirections(
                        startCoordinates = startCoordinates,
                        endCoordinates = endLocation.coordinate
                    )
                    val routes = displayRouteOnMap(routingDirections?.routes)

                    if (routes != null) {
                        // Pass the routes list to the bottom sheet so the user can make a selection
                        viewModel.route.value = routes
                    }

                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Navigation error: $e")
                }
            }
        } else {
            showToast(this, "Location is not set, cannot show route")
        }
    }

    override fun clearMarkers() {
        mapMarkersManager.clearMarkers()
    }

    override fun showMarkers(startLocation: Location?, endLocation: Location) {
        if (isPossibleRoute(startLocation))
            mapMarkersManager.showMarker(
                destination = endLocation.coordinate,
                start = startLocation?.coordinate ?: location,
                this
            ) else {
            showToast(this, "Location is not set, cannot show makers")
        }
    }

    private fun isPossibleRoute(startLocation: Location?): Boolean {
        //TODO: Refine this when location is turned off
        return startLocation != null || ::location.isInitialized
    }

    override fun routeChosen(route: Route) {
        displayRouteOnMap(routes = listOf(route))
    }
}
