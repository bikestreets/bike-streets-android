package com.application.bikestreets.bottomsheet

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.application.bikestreets.R
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.databinding.BottomDraggableSheetBinding
import com.application.bikestreets.utils.getSearchOptions
import com.application.bikestreets.utils.hideKeyboard
import com.application.bikestreets.utils.requestLocationPermission
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import kotlin.math.roundToInt

class BottomSheetFragment : Fragment() {
    private var _binding: BottomDraggableSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var currentBottomSheetState: BottomSheetStates = BottomSheetStates.INITIAL

    private lateinit var myTextWatcher: TextWatcher

    private var activeTextField: EditText? = null

    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter

    private lateinit var searchResultsView: SearchResultsView

    private lateinit var searchToEditText: EditText
    private lateinit var searchFromEditText: EditText

    private var startLocation: Location? = null
    private lateinit var endLocation: Location

    private var context: Activity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomDraggableSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        context = activity

        setBottomSheetBehavior()

        loadSearch()

        // enable settings button
        enableSettingsButton()

        // Show "center location" button if available
        enableFollowRiderButton()

        searchResultsView.isVisible = true

        return view
    }

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomNavigationContainer)

        updateBottomSheetPeekHeight()

        // X button will always collapse the bottom sheet
        binding.close.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.beginRoutingButton.setOnClickListener {
//            startRouting()
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // Hide keyboard if open
                        context?.let { hideKeyboard(it) }

                        binding.close.visibility = View.INVISIBLE
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.close.visibility = View.VISIBLE
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

    private fun loadSearch() {

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
        context?.let { requestLocationPermission(it) }
    }

    private fun initSearchEditText() {

        searchToEditText = binding.searchToEditText
        searchFromEditText = binding.searchFromEditText

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

    private fun showDirectionsBottomSheet() {
        if (currentBottomSheetState != BottomSheetStates.DIRECTIONS) {
            // TODO: don't re-run the visible code and peek height each time if already at this state
            // Change peek height before collapsing

            // Move drawer out of the way
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Reveal FROM location textbox
            searchFromEditText.visibility = View.VISIBLE
            setTextNoSearch(getString(R.string.current_location), searchFromEditText)

            // Reveal Search buttom
            binding.beginRoutingButton.visibility = View.VISIBLE

//            mapMarkersManager.showMarker(
//                destination = endLocation.coordinate,
//                start = startLocation?.coordinate ?: location,
//                this
//            )

            // Update helper text
            binding.vamosText.setText(R.string.search_directions)

            // We are now showing the "directions" version of the bottom sheet
            currentBottomSheetState = BottomSheetStates.DIRECTIONS
            updateBottomSheetPeekHeight()
        } else {
//            mapMarkersManager.clearMarkers()
//            mapMarkersManager.showMarker(
//                destination = endLocation.coordinate,
//                start = startLocation?.coordinate ?: location,
//                this
//            )
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

    private fun enableSettingsButton() {
//        // get the button
//        val settingsButton = binding.settings
//
//        // Set the callback in the fragment
//        val aboutFragment = AboutFragment()
//        aboutFragment.setOnPermissionRequested(this)
//
//        // show the button
//        settingsButton.visibility = View.VISIBLE
//
//        // enable the button's functionality
//        settingsButton.setOnClickListener {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.settings_fragment_container, aboutFragment).addToBackStack("null")
//                .commit()
//        }
    }

    private fun enableFollowRiderButton() {
//        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//            binding.followRider.visibility = View.VISIBLE
//
//            // enable the button's functionality
//            binding.followRider.setOnClickListener {
//                moveCamera(map = mapView.getMapboxMap(), location = location)
//            }
//        }
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

    private fun clearSearchText() {
        searchToEditText.text.clear()
        searchFromEditText.text.clear()
    }

    private fun startRouting() {
//
//        val startCoordinates =
//            startLocation?.coordinate ?: location
//
//
//        MainScope().launch(Dispatchers.Main) {
//            try {
//                val routingService = RoutingService()
//                val routingDirections = routingService.getRoutingDirections(
//                    startCoordinates = startCoordinates,
//                    endCoordinates = endLocation.coordinate
//                )
//                displayRouteOnMap(routingDirections)
//            } catch (e: Exception) {
//                Log.e(javaClass.simpleName, "Navigation error: $e")
//            }
//        }
//
//        // Collapse the bottom sheet
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

}