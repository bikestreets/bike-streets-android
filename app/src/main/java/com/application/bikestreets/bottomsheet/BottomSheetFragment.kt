package com.application.bikestreets.bottomsheet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.application.bikestreets.R
import com.application.bikestreets.SharedViewModel
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.composables.BottomSheetUi
import com.application.bikestreets.databinding.BottomDraggableSheetBinding
import com.application.bikestreets.utils.hideKeyboard
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.roundToInt

class BottomSheetFragment : Fragment() {
    private var _binding: BottomDraggableSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var currentBottomSheetState: BottomSheetStates = BottomSheetStates.INITIAL

    private lateinit var myTextWatcher: TextWatcher

    private var context: Activity? = null
    private lateinit var mListener: BottomSheetClickListener

    private val viewModel: SharedViewModel by activityViewModels()

    private fun updateUiWithNewRoutes(routes: List<Route>?) {
        Log.d("apples", routes.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomDraggableSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        context = activity

        setBottomSheetBehavior()

        // Load all composables
        loadSearch()

        // enable settings & location button
        enableSettingsButton()
        enableFollowRiderButton()

        return view
    }

    private fun onSearchOptionSelected(origin: Location?, destination: Location?) {
//        setStartOrEndLocation(location, activeTextField)
//        setTextNoSearch(location.name, activeTextField)
        showDirectionsBottomSheet(origin, destination)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as BottomSheetClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement BottomSheetClickListener")
        }
    }

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomNavigationContainer)

        updateBottomSheetPeekHeight()

        // X button will always collapse the bottom sheet
        binding.close.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

        val composeView = binding.composeView

        composeView.setContent {

            val routes by viewModel.route.observeAsState(initial = emptyList())

            BottomSheetUi(
                onSearchPerformed = { origin: Location?, destination: Location? ->
                    onSearchOptionSelected(
                        origin,
                        destination
                    )
                },
                routes = routes,
                notifyRouteChosen = { route -> notifyRouteChosen(route) }
            )
        }

        initSearchEditText()
    }

    private fun initSearchEditText() {
//
//        searchToEditText = binding.searchToEditText
//        searchFromEditText = binding.searchFromEditText
//
//        // On Initial state, assume all actions are for the destination
//        activeTextField = searchToEditText
//
//        val searchOptions = getSearchOptions()
//
//        myTextWatcher = object : TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//
//            }
//
//            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                searchEngineUiAdapter.search(newText.toString(), searchOptions)
//            }
//
//            override fun afterTextChanged(p0: Editable?) {
//
//            }
//        }
//
//        searchToEditText.addTextChangedListener(myTextWatcher)
//        searchFromEditText.addTextChangedListener(myTextWatcher)
//
//        // Expand the sheet when the user puts focus on the text box
//        searchToEditText.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            }
//
//            // Set active text to last touched search field
//            if (hasFocus) {
//                activeTextField = searchToEditText
//            }
//        }
//
//        searchFromEditText.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            }
//
//            // Set active text to last touched search field
//            if (hasFocus) {
//                activeTextField = searchFromEditText
//            }
//        }
    }

    private fun showDirectionsBottomSheet(origin: Location?, destination: Location?) {
        if (destination != null) {
            if (currentBottomSheetState != BottomSheetStates.DIRECTIONS) {

                // Move drawer out of the way
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // Reveal FROM location textbox
//            searchFromEditText.visibility = View.VISIBLE
//            setTextNoSearch(getString(R.string.current_location), searchFromEditText)

                mListener.showMarkers(origin, destination)

                // Update helper text
//                binding.vamosText.setText(R.string.search_directions)

                // We are now showing the "directions" version of the bottom sheet
                currentBottomSheetState = BottomSheetStates.DIRECTIONS
                updateBottomSheetPeekHeight()

                mListener.showRoutes(origin, destination)
            } else {
                mListener.clearMarkers()
                mListener.showMarkers(origin, destination)
                mListener.showRoutes(origin, destination)

                // We render on the map, but we want to show a start
            }
        } else {
            Log.e(javaClass.name, "Destination not set!, cannot show route on map")
        }
    }

    private fun setStartOrEndLocation(location: Location, activeTextField: EditText?) {
//        if (activeTextField != null) {
//            if (activeTextField == searchToEditText) {
//                endLocation = location
//            } else {
//                startLocation = location
//            }
//        } else {
//            Log.e(javaClass.simpleName, "No Text field is currently in focus!!")
//        }
    }

    private fun enableSettingsButton() {
        // Notify parent on click
        binding.settings.setOnClickListener {
            mListener.onSettingsButtonClicked()
        }
    }

    private fun enableFollowRiderButton() {
        binding.followRider.setOnClickListener {
            mListener.onFollowRiderButtonClicked()
        }
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
        }

        bottomSheetBehavior.peekHeight = totalOffset.roundToInt()
    }

    private fun clearSearchText() {
//        searchToEditText.text.clear()
//        searchFromEditText.text.clear()
    }

    private fun notifyRouteChosen(route: Route): () -> Unit {
        return {
            // Collapse the bottom sheet
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            mListener.routeChosen(route)
        }
    }
}