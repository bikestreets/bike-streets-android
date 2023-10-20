package com.application.bikestreets

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.api.modals.DirectionResponse
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Mode
import com.application.bikestreets.api.modals.Mode.Companion.getMode
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
import com.application.bikestreets.utils.getSearchOptions
import com.application.bikestreets.utils.hideCurrentRouteLayer
import com.application.bikestreets.utils.hideKeyboard
import com.application.bikestreets.utils.mapTypeFromPreferences
import com.application.bikestreets.utils.moveCamera
import com.application.bikestreets.utils.requestLocationPermission
import com.application.bikestreets.utils.showMapLayers
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
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
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

    private lateinit var locationEngine: LocationEngine
    private lateinit var location: Point

    private lateinit var searchToEditText: EditText
    private lateinit var searchFromEditText: EditText
    private var activeTextField: EditText? = null

    private var startLocation: Location? = null
    private lateinit var endLocation: Location

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var currentBottomSheetState: BottomSheetStates = BottomSheetStates.INITIAL

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter
    private lateinit var myTextWatcher: TextWatcher

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

        searchResultsView.isVisible = true
    }

    private fun initBottomNavigationSheet() {

        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.bottomNavigationContainer)

        updateBottomSheetPeekHeight()

        // X button will always collapse the bottom sheet
        binding.bottomSheet.close.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.bottomSheet.beginRoutingButton.setOnClickListener {
            startRouting()
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // Hide keyboard if open
                        hideKeyboard(this@MainActivity)

                        binding.bottomSheet.close.visibility = View.INVISIBLE
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.bottomSheet.close.visibility = View.VISIBLE
                    }

                    else -> {
                        // No custom actions needed for other states
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // React to dragging events
            }
        })
    }

    private fun updateBottomSheetPeekHeight() {
        // Set the peek height to only show search bar
        val dimenOffsetTappable =
            resources.getDimension(R.dimen.tappable_icons_height) * 2 + resources.getDimension(R.dimen.tappable_icons_vertical_padding) * 2
        val dimenPeekIndicator =
            resources.getDimension(R.dimen.draggable_indicator_height) + resources.getDimension(R.dimen.draggable_indicator_top_margin)
        val dimenClose =
            resources.getDimension(R.dimen.close_icon_height) + resources.getDimension(R.dimen.close_padding) * 2
        val dimenSearchEntry =
            resources.getDimension(R.dimen.search_icon_height) + resources.getDimension(R.dimen.edit_text_padding) * 2 + resources.getDimension(
                R.dimen.toolbar_vertical_margin
            )
        val dimenStartRoutingButton = resources.getDimension(R.dimen.button_height)

        val totalOffset = when (currentBottomSheetState) {
            BottomSheetStates.INITIAL -> (dimenOffsetTappable + dimenPeekIndicator + dimenClose + dimenSearchEntry)
            BottomSheetStates.DIRECTIONS -> (dimenOffsetTappable + dimenPeekIndicator + dimenClose + dimenSearchEntry * 2 + dimenStartRoutingButton)
            BottomSheetStates.ROUTE_SELECTION -> (dimenOffsetTappable + dimenPeekIndicator + dimenClose + dimenSearchEntry)
        }


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
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            binding.bottomSheet.followRider.visibility = View.VISIBLE

            // enable the button's functionality
            binding.bottomSheet.followRider.setOnClickListener {
                moveCamera(map = mapView.getMapboxMap(), location = location)
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
        // TODO: do this in a different thread so UI is not blocked
        mapMarkersManager = MapMarkersManager(mapView)

        loadSearch()
    }

    private fun showDirectionsBottomSheet() {
        if (currentBottomSheetState != BottomSheetStates.DIRECTIONS) {
            // TODO: don't re-run the visible code and peek height each time if already at this state
            // Change peek height before collapsing

            // Move drawer out of the way
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Reveal FROM location textbox
            searchFromEditText.visibility = View.VISIBLE
            setTextNoSearch(getString(R.string.current_location), searchFromEditText)

            // Reval Search buttom
            binding.bottomSheet.beginRoutingButton.visibility = View.VISIBLE

            mapMarkersManager.showMarker(
                destination = endLocation.coordinate,
                start = startLocation?.coordinate ?: location,
                this
            )

            // Update helper text
            binding.bottomSheet.vamosText.setText(R.string.search_directions)

            // We are now showing the "directions" version of the bottom sheet
            currentBottomSheetState = BottomSheetStates.DIRECTIONS
            updateBottomSheetPeekHeight()
        } else {
            mapMarkersManager.clearMarkers()
            mapMarkersManager.showMarker(
                destination = endLocation.coordinate,
                start = startLocation?.coordinate ?: location,
                this
            )
        }
    }

    private fun startRouting() {

        val startCoordinates =
            startLocation?.coordinate ?: location


        MainScope().launch(Dispatchers.Main) {
            try {
                val routingService = RoutingService()
                val routingDirections = routingService.getRoutingDirections(
                    startCoordinates = startCoordinates,
                    endCoordinates = endLocation.coordinate
                )
                displayRouteOnMap(routingDirections)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Navigation error: $e")
            }
        }

        // Collapse the bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
                // Do nothing
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
                setStartOrEndLocation(Location(searchResult), activeTextField)
                setTextNoSearch(searchResult.name, activeTextField)
                showDirectionsBottomSheet()
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                setStartOrEndLocation(Location(searchResult), activeTextField)
                setTextNoSearch(searchResult.name, activeTextField)
                showDirectionsBottomSheet()
            }

            override fun onError(e: Exception) {
                Log.e(javaClass.simpleName, "Mapbox Search Error: $e")
            }

            override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                // Not used
            }

            override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                setStartOrEndLocation(Location(historyRecord), activeTextField)
                setTextNoSearch(historyRecord.name, activeTextField)
                showDirectionsBottomSheet()
            }

            override fun onPopulateQueryClick(
                suggestion: SearchSuggestion,
                responseInfo: ResponseInfo
            ) {
                searchToEditText.setText(suggestion.name)
            }
        })

        initSearchEditText()
        requestLocationPermission(this)
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

        // TODO: add route selection step
        val selectedRoute = routingDirections?.routes?.first()

        val selectedRouteGeometry: MutableList<Feature> = mutableListOf()

        val legs = selectedRoute?.legs
        val steps = legs?.flatMap { it.steps }
        steps?.forEach {
            if (getMode(it.mode) == Mode.PUSHING_BIKE) {

                val mapBoxGeometry = convertToMapboxGeometry(it.geometry)
                val properties = JsonObject()
                properties.addProperty(
                    "stroke",
                    getColorHexString(this, R.color.sidewalk_segment)
                )

                selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
            } else {
                val mapBoxGeometry = convertToMapboxGeometry(it.geometry)
                val properties = JsonObject()
                properties.addProperty(
                    "stroke",
                    getColorHexString(this, R.color.vamos_light_blue)
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

    private fun clearSearchText() {
        searchToEditText.text.clear()
        searchFromEditText.text.clear()
    }

    /**
     * Native back button will do different actions based on what is open
     * 1st - clear out search and collapse bottomsheet
     * 2nd - if bottom sheet is collapsed, remove the currently shown route
     * 3rd - Close app if none of the above
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            // Clear search and collapse
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            clearSearchText()
        } else if (mapMarkersManager.hasMarkers) {
            hideCurrentRouteLayer(mapView.getMapboxMap())
            mapMarkersManager.clearMarkers()
        } else {
            super.onBackPressed()
        }
    }

    private fun initSearchEditText() {

        searchToEditText = binding.bottomSheet.searchToEditText
        searchFromEditText = binding.bottomSheet.searchFromEditText

        // On Initial state, assume all actions are for the destination
        activeTextField = searchToEditText

        val searchOptions = getSearchOptions()

        myTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchEngineUiAdapter.search(newText.toString(), searchOptions)
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        searchToEditText.addTextChangedListener(myTextWatcher)
        searchFromEditText.addTextChangedListener(myTextWatcher)

        // Expand the sheet when the user puts focus on the text box
        //TODO : Can put use .setCompoundDrawablesRelativeWithIntrinsicBounds to change the icon while focused
        searchToEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            // Set active text to last touched search field
            if (hasFocus) {
                activeTextField = searchToEditText
            }
        }

        searchFromEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            // Set active text to last touched search field
            if (hasFocus) {
                activeTextField = searchFromEditText
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                setScreenModeFromPreferences()
            }

            MAP_TYPE_PREFERENCE_KEY -> {
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
                showToast(this, getString(R.string.no_location_access))
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

    // Temporarily remove the text listener to set the text without performing a search
    private fun setTextNoSearch(name: String, activeTextField: EditText?) {
        if (activeTextField != null) {
            activeTextField.removeTextChangedListener(myTextWatcher)
            activeTextField.setText(name)
            activeTextField.addTextChangedListener(myTextWatcher)
        } else {
            Log.e(javaClass.simpleName, "No Text field is currently in focus!")
        }

    }


    private fun setStartOrEndLocation(location: Location, activeTextField: EditText?) {
        if (activeTextField != null) {
            if (activeTextField == searchToEditText) {
                endLocation = location
            } else {
                startLocation = location
            }
        } else {
            Log.e(javaClass.simpleName, "No Text field is currently in focus!!")
        }
    }

}
