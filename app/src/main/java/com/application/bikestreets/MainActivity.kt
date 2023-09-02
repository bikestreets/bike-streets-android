package com.application.bikestreets

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.api.modals.DirectionResponse
import com.application.bikestreets.constants.PreferenceConstants.KEEP_SCREEN_ON_PREFERENCE_KEY
import com.application.bikestreets.constants.PreferenceConstants.MAP_TYPE_PREFERENCE_KEY
import com.application.bikestreets.databinding.ActivityMainBinding
import com.application.bikestreets.utils.ToastUtils.showToast
import com.application.bikestreets.utils.userDistanceTo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
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
import com.mapbox.search.ui.view.place.SearchPlace
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.InputStream


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    AboutFragment.OnPermissionButtonClickListener {
    private lateinit var mapView: MapView
    private val activity: MainActivity = this

    private lateinit var locationEngine: LocationEngine
    private lateinit var location: Point

    private lateinit var actionSearch: Toolbar
    private lateinit var searchView: SearchView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter
    private lateinit var searchPlaceView: SearchPlaceBottomSheetView

    private lateinit var mapMarkersManager: MapMarkersManager
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var defaultPackage: String

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        locationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.bottomNavigationContainer)
//        bottomSheetBehavior.peekHeight = binding.bottomSheet.appBar.height
        setupBottomSheet()

        setContentView(view)

        // Carry over from getDefaultSharedPreferences()
        defaultPackage = "${packageName}_preferences"
        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        // enable settings button
        enableSettingsButton()

        // Show "center location" button if available
        enableFollowRiderButton()

        mapView = binding.mapView
        setupMapboxMap()
        setupPolyLines()
    }

    private fun setScreenModeFromPreferences() {
        sharedPreferences = getSharedPreferences(defaultPackage, Context.MODE_PRIVATE)

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
        val settingsButton = binding.settings

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
            binding.followRider.visibility = View.VISIBLE

            // enable the button's functionality
            binding.followRider.setOnClickListener {
                moveCamera(location)
            }
        }
    }

    private fun showMapLayers(activity: MainActivity, mapStyle: Style) {
        val root = "geojson"
        val mAssetManager = activity.assets

        mAssetManager.list("$root/")?.forEach { fileName ->
            val featureCollection = featureCollectionFromStream(
                mAssetManager.open("$root/$fileName")
            )

            renderFeatureCollection(fileName, featureCollection, mapStyle)
        }

    }

    private fun featureCollectionFromStream(fileStream: InputStream): FeatureCollection {
        val geoJsonString = StringToStream.convert(fileStream)

        return FeatureCollection.fromJson(geoJsonString)
    }

    private fun colorForLayer(layerName: String): Int {
        // This is lazy coupling and will break, but I want to see it work as a proof-of-concept.
        // A more flexible refactor involves inspecting the GeoJson file itself to get the layer
        // name, then matching the color based on that (or we can save the layer color as metadata.)
        val lineColor = when (layerName) {
            "terms_of_use.txt" -> R.color.mapTrails
            "1-bikestreets-master-v0.3.geojson" -> R.color.mapBikeStreets
            "2-trails-master-v0.3.geojson" -> R.color.mapTrails
            "3-bikelanes-master-v0.3.geojson" -> R.color.mapBikeLane
            "4-bikesidewalks-master-v0.3.geojson" -> R.color.mapRideSidewalk
            "5-walk-master-v0.3.geojson" -> R.color.mapWalkSidewalk
            else -> R.color.mapDefault
        }

        // convert line color from R.color format to a more standard color format that
        // PropertyFactory.lineColor knows how to work with
        return ContextCompat.getColor(this, lineColor)
    }

    private fun createLineLayer(layerName: String): LineLayer {
        val lineColor = colorForLayer(layerName)

        return LineLayer("$layerName-id", layerName).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND)
            .lineOpacity(1f.toDouble()).lineWidth(interpolate {
                linear()
                zoom()
                stop {
                    literal(8)
                    literal(0.2f.toDouble())
                }
                stop {
                    literal(16)
                    literal(10f.toDouble())
                }
            }).lineColor(lineColor)
    }

    private fun renderFeatureCollection(
        layerName: String, featureCollection: FeatureCollection, mapStyle: Style
    ) {
        if (featureCollection.features() != null) {
            // add the data itself to mapStyle
            mapStyle.addSource(
                GeoJsonSource.Builder(layerName).featureCollection(featureCollection).build()
            )

            if (mapTypeFromPreferences().equals(getString(R.string.preference_satellite))) {
                //TODO: In satellite view, routes appear above the navigation line
                mapStyle.addLayer(createLineLayer(layerName))
            } else {
                // create a line layer that reads the GeoJSON data that we just added
                mapStyle.addLayerBelow(createLineLayer(layerName), "road-label")
            }

        }
    }

    private fun mapTypeFromPreferences(): String? {
        val sharedPreferences = getSharedPreferences(defaultPackage, Context.MODE_PRIVATE)
        return sharedPreferences.getString(
            MAP_TYPE_PREFERENCE_KEY,
            getString(R.string.preference_street)
        )
    }

    private fun setupPolyLines() {
        // Create an instance of the Annotation API and get the polygon manager.
        val annotationApi = mapView.annotations
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
    }

    private fun setupMapboxMap() {

        mapView.getMapboxMap().also { mapboxMap ->

            loadMapboxStyle(mapboxMap)
            loadLocation()
        }

        // Load Map Markers
        mapMarkersManager = MapMarkersManager(mapView)
        mapMarkersManager.onMarkersChangeListener = {
            updateOnBackPressedCallbackEnabled()
        }

        actionSearch = binding.bottomSheet.searchView.apply {
            title = "Search"
            setSupportActionBar(this)
        }

        loadSearch()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    Log.d(javaClass.simpleName, "expanded")
                    // do stuff when the drawer is expanded
                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // do stuff when the drawer is collapsed
                    Log.d(javaClass.simpleName, "collapsed")
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // do stuff during the actual drag event for example
                // animating a background color change based on the offset

                // or for example hidding or showing a fab
                if (slideOffset > 0.2) {
//                    if (fab.isShown()) {
//                        fab.hide()
//                    }
                    Log.d(javaClass.simpleName, "0.2")
                } else if (slideOffset < 0.15) {
//                    if (!fab.isShown()) {
//                        fab.show()
                    Log.d(javaClass.simpleName, "0.15")
//                    }
                }
            }
        })
    }

    private fun loadSearch() {
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
                closeSearchView()
                searchPlaceView.open(SearchPlace.createFromSearchResult(searchResult, responseInfo))
                mapMarkersManager.showMarker(searchResult.coordinate, activity)
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                closeSearchView()
                searchPlaceView.open(SearchPlace.createFromOfflineSearchResult(searchResult))
                mapMarkersManager.showMarker(searchResult.coordinate, activity)
            }

            override fun onError(e: Exception) {
                Log.e(javaClass.simpleName, "Error happened: $e")
            }

            override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                // No used
            }

            override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                closeSearchView()
                searchPlaceView.open(
                    SearchPlace.createFromIndexableRecord(
                        historyRecord,
                        distanceMeters = null
                    )
                )

                locationEngine.userDistanceTo(
                    activity,
                    historyRecord.coordinate
                ) { distance ->
                    distance?.let {
                        searchPlaceView.updateDistance(distance)
                    }
                }

                mapMarkersManager.showMarker(historyRecord.coordinate, activity)
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

        searchPlaceView = binding.searchPlaceCard
        // Hidden by default
        searchPlaceView.initialize(CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL))

        searchPlaceView.addOnCloseClickListener {
            searchPlaceView.hide()
        }

        searchPlaceView.isFavoriteButtonVisible = false
        searchPlaceView.isShareButtonVisible = false

        searchPlaceView.addOnNavigateClickListener { searchPlace ->
            MainScope().launch(Dispatchers.Main) {
                try {
                    val routingDirections = RoutingService.getRoutingDirections(
                        startCoordinates = location,
                        endCoordinates = searchPlace.coordinate
                    )
                    displayRouteOnMap(routingDirections)
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Navigation error: $e")
                    // Handle errors
                }
            }
        }

        searchPlaceView.addOnShareClickListener {
            // Not implemented
        }

        searchPlaceView.addOnFeedbackClickListener { _, _ ->
            // Not implemented
        }

        searchPlaceView.addOnBottomSheetStateChangedListener { _, _ ->
            updateOnBackPressedCallbackEnabled()
        }

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSIONS_REQUEST_LOCATION
        )
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
                    //TODO if follow setting is enabled
                    moveCamera(location)

                    mapView.location.removeOnIndicatorPositionChangedListener(this)
                }
            })
        }
    }

    private fun loadMapboxStyle(mapboxMap: MapboxMap) {
        var mapStyle = "asset://stylejson/style.json"

        // apply map style conditionally, based on user's preferences.
        if (mapTypeFromPreferences().equals(getString(R.string.preference_satellite))) {
            mapStyle = Style.SATELLITE
        }

        // Load style, on compete show layers
        mapboxMap.loadStyleUri(mapStyle) { showMapLayers(this, it) }
    }

    // Once a search has kicked off, given the response API, we use that route to draw a polyline
    private fun displayRouteOnMap(routingDirections: DirectionResponse?) {
        val selectedRoute = routingDirections?.routes?.first()
        val legs = selectedRoute?.legs
        val steps = legs?.flatMap { it.steps }
        val coordinateList = steps?.flatMap { it.geometry.coordinates }

        // Convert from List<List<Double>> to List<Point> for use with mapbox functions
        val pointsList: List<Point>? = coordinateList?.map { coordinate ->
            fromLngLat(coordinate[0], coordinate[1])
        }

        // Remove previous polylines shown on map
        polylineAnnotationManager.deleteAll()

        pointsList?.let {
            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(it)
                // Style the polyline that will be added to the map.
                .withLineColor("#47F0F5")
                .withLineJoin(LineJoin.ROUND)
                .withLineWidth(6.0)

            // Add the resulting polygon to the map.
            polylineAnnotationManager.create(polylineAnnotationOptions)
        }
    }

    private fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled =
            !searchPlaceView.isHidden() || mapMarkersManager.hasMarkers
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

        const val PERMISSIONS_REQUEST_LOCATION = 0
    }


    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                !searchPlaceView.isHidden() -> {
                    mapMarkersManager.clearMarkers()
                    searchPlaceView.hide()
                }

                mapMarkersManager.hasMarkers -> {
                    mapMarkersManager.clearMarkers()
                }

                else -> {
                    Log.i("SearchApiExample", "This OnBackPressedCallback should not be enabled")
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_options_menu, menu)

        val searchActionView = menu.findItem(R.id.action_search)
        searchActionView.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                searchPlaceView.hide()
                searchResultsView.isVisible = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchResultsView.isVisible = false
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

    fun moveCamera(location: Point) {
        // TODO: ease animation to smooth out movement
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(location)
                .build()
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
        Log.d(javaClass.simpleName, "fragment click!!")
        requestLocationPermission()
    }
}
