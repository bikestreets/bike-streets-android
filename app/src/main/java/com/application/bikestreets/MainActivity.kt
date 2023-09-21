package com.application.bikestreets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.api.modals.DirectionResponse
import com.application.bikestreets.api.modals.Mode
import com.application.bikestreets.api.modals.Mode.Companion.getMode
import com.application.bikestreets.constants.MapLayerConstants.SELECTED_ROUTE_MAP_LAYER
import com.application.bikestreets.constants.PreferenceConstants.KEEP_SCREEN_ON_PREFERENCE_KEY
import com.application.bikestreets.constants.PreferenceConstants.MAP_TYPE_PREFERENCE_KEY
import com.application.bikestreets.databinding.ActivityMainBinding
import com.application.bikestreets.utils.PERMISSIONS_REQUEST_LOCATION
import com.application.bikestreets.utils.addLayerBasedOnMapType
import com.application.bikestreets.utils.convertToMapboxGeometry
import com.application.bikestreets.utils.getColorHexString
import com.application.bikestreets.utils.getDefaultPackageName
import com.application.bikestreets.utils.hideCurrentRouteLayer
import com.application.bikestreets.utils.mapTypeFromPreferences
import com.application.bikestreets.utils.requestLocationPermission
import com.application.bikestreets.utils.showMapLayers
import com.application.bikestreets.utils.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.search.ApiType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.offline.OfflineResponseInfo
import com.mapbox.search.offline.OfflineSearchEngine
import com.mapbox.search.offline.OfflineSearchEngineSettings
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.adapter.engines.SearchEngineUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchMode
import com.mapbox.search.ui.view.SearchResultsView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    AboutFragment.OnPermissionButtonClickListener {
    private lateinit var mapView: MapView
    private val activity: MainActivity = this

    private lateinit var locationEngine: LocationEngine
    private lateinit var location: Point

    private lateinit var actionSearch: Toolbar
    private lateinit var searchView: SearchView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter

    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        initBottomNavigationSheet()

        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        mapView = binding.mapView
        setupMapboxMap()

        // enable settings button
        enableSettingsButton()

        // Show "center location" button if available
        enableFollowRiderButton()
    }

    private fun initBottomNavigationSheet() {

        // Set the peek height to only show search bar
        val dimenOffsetTappable =
            resources.getDimension(R.dimen.tappable_icons_height) * 2 + resources.getDimension(R.dimen.tappable_icons_vertical_padding) * 2
        val dimenPeekIndicator =
            resources.getDimension(R.dimen.draggable_indicator_height) + resources.getDimension(R.dimen.draggable_indicator_vertical_margin)
        val dimenSearchTool =
            resources.getDimension(R.dimen.search_view_height) + resources.getDimension(R.dimen.toolbar_vertical_margin) * 2

        val totalOffset = dimenOffsetTappable + dimenPeekIndicator + dimenSearchTool

        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.bottomNavigationContainer)

        bottomSheetBehavior.peekHeight = totalOffset.roundToInt()
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

    private fun enableSettingsButton() {
        // get the button
        val settingsButton = binding.bottomSheet.settings

        // Set the callback in the fragment
        val aboutFragment = AboutFragment()
        aboutFragment.setOnPermissionRequested(this)

        // show the button
        settingsButton.visibility = View.VISIBLE

        // enable the button's functionality
        settingsButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, aboutFragment).addToBackStack("null")
                .commit()
        }
    }

    private fun enableFollowRiderButton() {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            binding.bottomSheet.followRider.visibility = View.VISIBLE

            // enable the button's functionality
            binding.bottomSheet.followRider.setOnClickListener {
                moveCamera(location)
            }
        }
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
            loadMapboxStyle(mapboxMap)
            loadLocation()
        }

        // Load Map Markers
        mapMarkersManager = MapMarkersManager(mapView)

        actionSearch = binding.bottomSheet.searchView.apply {
            title = "Search"
            setSupportActionBar(this)
        }

        loadSearch()
    }

    private fun startSearchRouting(coordinate: Point) {
        closeSearchView()
        mapMarkersManager.showMarker(coordinate, activity)

        MainScope().launch(Dispatchers.Main) {
            try {
                val routingDirections = RoutingService.getRoutingDirections(
                    startCoordinates = location,
                    endCoordinates = coordinate
                )
                displayRouteOnMap(routingDirections)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Navigation error: $e")
                // Handle errors
            }
        }
    }

    private fun loadSearch() {
        //TODO: limit search to be bound to the region (https://docs.mapbox.com/android/search/api/core/1.0.0-rc.6/sdk/com.mapbox.search/-search-options/)

        val apiType = ApiType.GEOCODING

        searchResultsView = binding.bottomSheet.searchResultsView.apply {
            initialize(
                SearchResultsView.Configuration(CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL))
            )
            isVisible = false
        }

        val searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            apiType = apiType,
            settings = SearchEngineSettings(getString(R.string.mapbox_access_token))
        )

        val offlineSearchEngine = OfflineSearchEngine.create(
            OfflineSearchEngineSettings(getString(R.string.mapbox_access_token))
        )

        searchEngineUiAdapter = SearchEngineUiAdapter(
            view = searchResultsView,
            searchEngine = searchEngine,
            offlineSearchEngine = offlineSearchEngine,
        )

        searchEngineUiAdapter.searchMode = SearchMode.AUTO
        searchEngineUiAdapter.addSearchListener(object : SearchEngineUiAdapter.SearchListener {

            override fun onSuggestionsShown(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
                // Nothing to do
            }

            override fun onSearchResultsShown(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                closeSearchView()
                mapMarkersManager.showMarkers(results.map { it.coordinate }, activity)
            }

            override fun onOfflineSearchResultsShown(
                results: List<OfflineSearchResult>,
                responseInfo: OfflineResponseInfo
            ) {
                // Nothing to do
            }

            override fun onSuggestionSelected(searchSuggestion: SearchSuggestion): Boolean {
                return false
            }

            override fun onSearchResultSelected(
                searchResult: SearchResult,
                responseInfo: ResponseInfo
            ) {
                startSearchRouting(searchResult.coordinate)
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                startSearchRouting(searchResult.coordinate)
            }

            override fun onError(e: Exception) {
                Log.e(javaClass.simpleName, "Mapbox Search Error: $e")
            }

            override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                // No used
            }

            override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                startSearchRouting(historyRecord.coordinate)
            }

            override fun onPopulateQueryClick(
                suggestion: SearchSuggestion,
                responseInfo: ResponseInfo
            ) {
                if (::searchView.isInitialized) {
                    searchView.setQuery(suggestion.name, true)
                }
            }
        })

        requestLocationPermission(this)
    }

    private fun loadLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            mapView.location.updateSettings {
                enabled = true
            }

            mapView.location.addOnIndicatorPositionChangedListener(object :
                OnIndicatorPositionChangedListener {
                override fun onIndicatorPositionChanged(point: Point) {
                    location = point
                    moveCamera(location)

                    mapView.location.removeOnIndicatorPositionChangedListener(this)
                }
            })
        }
    }

    private fun loadMapboxStyle(mapboxMap: MapboxMap) {
        var mapStyle = "asset://stylejson/style.json"

        // apply map style conditionally, based on user's preferences.
        if (mapTypeFromPreferences(this).equals(getString(R.string.preference_satellite))) {
            mapStyle = Style.SATELLITE
        }

        // Load style, on compete show layers
        mapboxMap.loadStyleUri(mapStyle) { showMapLayers(this, it) }
    }

    // Once a search has kicked off, given the response API, we use that route to draw a polyline
    private fun displayRouteOnMap(routingDirections: DirectionResponse?) {

        val mapStyle = mapView.getMapboxMap().getStyle()

        //TODO: add route selection step
        val selectedRoute = routingDirections?.routes?.first()

        val selectedRouteGeometry: MutableList<Feature> = mutableListOf()

        val legs = selectedRoute?.legs
        val steps = legs?.flatMap { it.steps }
        steps?.forEach {
            if (getMode(it.mode) == Mode.PUSHING_BIKE) {

                val mapBoxGeometry = convertToMapboxGeometry(it.geometry)
                val properties = JsonObject()
                properties.addProperty("stroke", getColorHexString(this, R.color.sidewalk_segment))

                selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
            } else {
                val mapBoxGeometry = convertToMapboxGeometry(it.geometry)
                val properties = JsonObject()
                properties.addProperty(
                    "stroke",
                    getColorHexString(this, R.color.primary_route_segment)
                )

                selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
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
    }

    private fun closeSearchView() {
        actionSearch.collapseActionView()
        searchView.setQuery("", false)
    }

    private class MapMarkersManager(mapView: MapView) {

        private val mapboxMap = mapView.getMapboxMap()
        private val circleAnnotationManager =
            mapView.annotations.createCircleAnnotationManager(null)
        private val markers = mutableMapOf<Long, Point>()

        var onMarkersChangeListener: (() -> Unit)? = null

        val hasMarkers: Boolean
            get() = markers.isNotEmpty()

        fun clearMarkers() {
            markers.clear()
            circleAnnotationManager.deleteAll()
        }

        fun showMarker(coordinate: Point, context: Context) {
            showMarkers(listOf(coordinate), context)
        }

        fun showMarkers(coordinates: List<Point>, context: Context) {
            clearMarkers()
            if (coordinates.isEmpty()) {
                onMarkersChangeListener?.invoke()
                return
            }

            coordinates.forEach { coordinate ->
                val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                    .withPoint(coordinate)
                    .withCircleRadius(8.0)
                    .withCircleColor(ContextCompat.getColor(context, R.color.destination_pin))
                    .withCircleStrokeWidth(3.0)
                    .withCircleStrokeColor("#ffffff")

                val annotation = circleAnnotationManager.create(circleAnnotationOptions)
                markers[annotation.id] = coordinate
            }

            if (coordinates.size == 1) {
                CameraOptions.Builder()
                    .center(coordinates.first())
                    .padding(MARKERS_INSETS_OPEN_CARD)
                    .zoom(14.0)
                    .build()
            } else {
                mapboxMap.cameraForCoordinates(
                    coordinates, MARKERS_INSETS, bearing = null, pitch = null
                )
            }.also {
                mapboxMap.setCamera(it)
            }
            onMarkersChangeListener?.invoke()
        }
    }

    private companion object {

        val MARKERS_EDGE_OFFSET = Utils.dpToPx(64F).toDouble()
        val PLACE_CARD_HEIGHT = Utils.dpToPx(300F).toDouble()

        //TODO Adjust camera so both start and end are visible
        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET
        )

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            collapseBottomSheet()
        } else if (mapMarkersManager.hasMarkers) {
            hideCurrentRouteLayer(mapView.getMapboxMap())
            mapMarkersManager.clearMarkers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_options_menu, menu)

        searchResultsView.isVisible = true

        val searchActionView = menu.findItem(R.id.action_search)
        searchActionView.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                expandBottomSheet()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                collapseBottomSheet()
                return true
            }
        })

        searchView = searchActionView.actionView as SearchView
        searchView.queryHint = "where to?"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchEngineUiAdapter.search(newText)
                return false
            }
        })
        return true
    }

    private fun expandBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun collapseBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun moveCamera(location: Point) {
        mapView.getMapboxMap().easeTo(
            cameraOptions = CameraOptions.Builder()
                .center(location)
                .build(),
            animationOptions = mapAnimationOptions {
                duration(500)
                interpolator(FastOutSlowInInterpolator())
            }
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                setScreenModeFromPreferences()
            }

            MAP_TYPE_PREFERENCE_KEY -> {
                Log.d(javaClass.simpleName, "Updating style")
                // call this function, only to update the map style
                loadMapboxStyle(mapView.getMapboxMap())
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
                enableFollowRiderButton()
            } else {
                showToast(activity, getString(R.string.no_location_access))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    // Triggered when button clicked in settings fragment
    override fun onPermissionButtonClicked() {
        requestLocationPermission(this)
    }
}
