package com.application.bikestreets

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.databinding.ActivityMainBinding
import com.application.bikestreets.utils.userDistanceTo
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
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

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var permissionsManager: PermissionsManager? = null
    private val activity: MainActivity = this

    private lateinit var locationEngine: LocationEngine

    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter
    private lateinit var searchPlaceView: SearchPlaceBottomSheetView

    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        locationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        // enable settings button
        enableSettingsButton()

        mapView = binding.mapView
        setupMapboxMap()
    }

    private fun setScreenModeFromPreferences() {
        // extract string from strings.xml file (as integer key) and convert to string
        val keepScreenOnKey = getResources().getString(R.string.keep_screen_preference_key)

        // extracted stored value for user's screen mode preference
        // TODO: remove depreciated code
        val keepScreenOnPreference = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(keepScreenOnKey, true) // default is to keep the screen on

        // adding this flag will keep the screen from turning off if the app goes idle
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (keepScreenOnPreference) {
            window.addFlags(keepScreenOnFlag)
        } else {
            window.clearFlags(keepScreenOnFlag)
        }
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

        // show the button
        settingsButton.visibility = View.VISIBLE

        // enable the button's functionality
        settingsButton.setOnClickListener {
            val intent = Intent(this, About::class.java).apply {}
            startActivity(intent)
        }
    }

    private fun enableFollowRiderButton() {
        //TODO: fix this
//        // get the button
//        val followRiderButton = binding.followRider
//
//        // show the button
//        followRiderButton.visibility = View.VISIBLE
//
//        // enable the button's functionality
//        followRiderButton.setOnClickListener {
//            val locationComponent = mapView?.location
//            mapView?.getMapboxMap()?.setCamera(locationComponent)
//        }
    }

    private fun checkLocationPermission(onMapReady: () -> Unit) {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            onMapReady()
        } else {
            val permissionsListener: PermissionsListener = object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        onMapReady()
                    } else {
                        // User denied the permission: don't put a point on the map at all
                        activity.finish()
                    }
                }
            }

            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager?.requestLocationPermissions(activity)
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

            if (mapTypeFromPreferences() == "satellite_view") {
                mapStyle.addLayer(createLineLayer(layerName))
            } else {
                // create a line layer that reads the GeoJSON data that we just added
                mapStyle.addLayerBelow(createLineLayer(layerName), "road-label")
            }

        }
    }

//    private fun cameraModeFromPreferences(): Int {
//        // extract string from strings.xml file (as integer key) and convert to string
//        val orientataionPreferenceKey = getResources().getString(R.string.map_orientation_preference_key)
//
//        // use key to extract saved camera mode preference string. Default to tracking compass,
//        // a.k.a. "Direction of Travel"
//        val orientationPreferenceString = PreferenceManager
//            .getDefaultSharedPreferences(this)
//            .getString(orientataionPreferenceKey, "direction_of_travel")
//
//       // convert into a MapBox camera mode and return
//        return if (orientationPreferenceString == "fixed") {
//            CameraMode.TRACKING
//        } else {
//            CameraMode.TRACKING_COMPASS
//        }
//    }

    private fun drawLocationOnMap() {

//        binding.mapView.location.apply {
//            locationPuck = createDefault2DPuck(activity, withBearing = true)

        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                topImage = AppCompatResources.getDrawable(
                    activity, com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
                ), bearingImage = AppCompatResources.getDrawable(
                    activity,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon,
                ), shadowImage = AppCompatResources.getDrawable(
                    activity,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon,
                ), scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
    }

    private fun mapTypeFromPreferences(): String? {
        // extract preference key string from strings.xml
        val mapTypePreferenceKey = getResources().getString(R.string.map_type_preference_key)

        // use that key to extract the stored user preference
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getString(mapTypePreferenceKey, "street_map")
    }

    private fun addRoutesAndLocation(style: Style) {
        // add geojson layers
        showMapLayers(this, style)

        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        checkLocationPermission {
            enableFollowRiderButton()
            drawLocationOnMap()
        }

    }

    private fun setupMapboxMap() {

        mapView.getMapboxMap().also { mapboxMap ->

            var mapStyle = "asset://stylejson/style.json"

            // apply map style conditionally, based on user's preferences.
            if (mapTypeFromPreferences() == "satellite_view") {
                mapStyle = Style.SATELLITE
            }

            // Load style, on compete show layers
            mapboxMap.loadStyleUri(mapStyle) { showMapLayers(this, it) }

            mapView.location.updateSettings {
                enabled = true
            }

            mapView.location.addOnIndicatorPositionChangedListener(object :
                OnIndicatorPositionChangedListener {
                override fun onIndicatorPositionChanged(point: Point) {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(14.0)
                            .build()
                    )

                    mapView.location.removeOnIndicatorPositionChangedListener(this)
                }
            })
        }

        mapMarkersManager = MapMarkersManager(mapView)
        mapMarkersManager.onMarkersChangeListener = {
            updateOnBackPressedCallbackEnabled()
        }

        toolbar = binding.toolbar.apply {
            title = "Search"
            setSupportActionBar(this)
        }

        val apiType = ApiType.GEOCODING


        searchResultsView = binding.searchResultsView.apply {
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
                mapMarkersManager.showMarkers(results.map { it.coordinate })
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
                mapMarkersManager.showMarker(searchResult.coordinate)
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                closeSearchView()
                searchPlaceView.open(SearchPlace.createFromOfflineSearchResult(searchResult))
                mapMarkersManager.showMarker(searchResult.coordinate)
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

                mapMarkersManager.showMarker(historyRecord.coordinate)
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

        searchPlaceView = binding.searchPlaceView
        searchPlaceView.initialize(CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL))

        searchPlaceView.addOnCloseClickListener {
            mapMarkersManager.clearMarkers()
            searchPlaceView.hide()
        }

        searchPlaceView.isFavoriteButtonVisible = false
        searchPlaceView.isShareButtonVisible = false

        searchPlaceView.addOnNavigateClickListener { searchPlace ->
            MainScope().launch(Dispatchers.IO) {
                try {
                    Log.i(javaClass.simpleName, "Begin navigation:  ${searchPlace.coordinate}")
                    val startCoordinates: Point = fromLngLat(-104.990251, 39.7392358)
                    val routingDirections = RoutingService.getRoutingDirections(
                        startCoordinates = startCoordinates,
                        endCoordinates = searchPlace.coordinate
                    )
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "searchPlaceView error: $e")
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

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled =
            !searchPlaceView.isHidden() || mapMarkersManager.hasMarkers
    }

    private fun closeSearchView() {
        toolbar.collapseActionView()
        searchView.setQuery("", false)
    }

    override fun onResume() {
        super.onResume()

//        Log.w("Apples", "On Resume Called")
//        // if the user is returning from the settings page, those settings will need to be applied
//        // TODO: solve onResume getting called in a loop
//        setupMapboxMap(updateStyleOnly = true)
//
//        // reload the autosleep preference in case it was changed while the activity was paused
//        setScreenModeFromPreferences()
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

        fun showMarker(coordinate: Point) {
            showMarkers(listOf(coordinate))
        }

        fun showMarkers(coordinates: List<Point>) {
            clearMarkers()
            if (coordinates.isEmpty()) {
                onMarkersChangeListener?.invoke()
                return
            }

            coordinates.forEach { coordinate ->
                val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                    .withPoint(coordinate)
                    .withCircleRadius(8.0)
                    .withCircleColor("#ee4e8b")
                    .withCircleStrokeWidth(2.0)
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
}

