package com.application.bikestreets.bottomsheet

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.application.bikestreets.R
import com.application.bikestreets.SharedViewModel
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.composables.ActionButtonsContainer
import com.application.bikestreets.composables.BottomSheet
import com.application.bikestreets.composables.BottomSheetContent

class BottomSheetFragment : Fragment() {

    private lateinit var mListener: BottomSheetClickListener

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {

            setContent {

                val routes: List<Route> by viewModel.route.observeAsState(initial = emptyList())


                BottomSheet(
                    sheetContent = {
                        BottomSheetContent(
                            onSearchPerformed = { origin: Location?, destination: Location? ->
                                onSearchOptionSelected(
                                    origin,
                                    destination
                                )
                            },
                            routes = routes,
                            notifyRouteChosen = { route -> notifyRouteChosen(route) },
                            onCloseClicked = { closeBottomSheet() }
                        )
                    },
                    actionButtons = {
                        ActionButtonsContainer(
                            onSettingsButtonClicked = { mListener.onSettingsButtonClicked() },
                            onLocationButtonClicked = { mListener.onLocationButtonClicked() }
                        )
                    },
                    closeBottomSheet = { notifyOfBottomSheetClose() }
                )
            }
        }
    }

    private fun notifyOfBottomSheetClose() {
        TODO("Not yet implemented")
    }

    private fun closeBottomSheet() {
        TODO("Not yet implemented")
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
//        bottomSheetBehavior =
//            BottomSheetBehavior.from(binding.bottomNavigationContainer)
//
//
//        // X button will always collapse the bottom sheet
//        binding.close.setOnClickListener {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//        }
//
//        bottomSheetBehavior.addBottomSheetCallback(object :
//            BottomSheetBehavior.BottomSheetCallback() {
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                        // Hide keyboard if open
//                        context?.let { hideKeyboard(it) }
//
//                        binding.close.visibility = View.INVISIBLE
//                    }
//
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        binding.close.visibility = View.VISIBLE
//                    }
//
//                    else -> {
//                        // No custom actions needed for other states
//                    }
//                }
//            }
//
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                // React to dragging events
//            }
//        })
    }

    private fun showDirectionsBottomSheet(origin: Location?, destination: Location?) {
        if (destination != null) {
            if (viewModel.bottomSheetState.value != BottomSheetStates.DIRECTIONS) {

                // Move drawer out of the way
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // Reveal FROM location textbox
//            searchFromEditText.visibility = View.VISIBLE
//            setTextNoSearch(getString(R.string.current_location), searchFromEditText)

                mListener.showMarkers(origin, destination)

                // Update helper text
//                binding.vamosText.setText(R.string.search_directions)

                // We are now showing the "directions" version of the bottom sheet
                viewModel.bottomSheetState.value = BottomSheetStates.DIRECTIONS
                updateBottomSheetPeekHeight()

                mListener.showRoutes(origin, destination)
            } else {
                mListener.clearMarkers()
                mListener.showMarkers(origin, destination)
                mListener.showRoutes(origin, destination)
            }
        } else {
            Log.e(javaClass.name, "Destination not set!, cannot show route on map")
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

        val totalOffset = when (viewModel.bottomSheetState.value) {
            BottomSheetStates.INITIAL -> (dimenOffsetTappable + dimenPeekIndicator + dimenClose + dimenSearchEntry)
            BottomSheetStates.DIRECTIONS -> (dimenOffsetTappable + dimenPeekIndicator + dimenClose + dimenSearchEntry * 2 + dimenStartRoutingButton)
            else -> 0f
        }

//        bottomSheetBehavior.peekHeight = totalOffset.roundToInt()
    }

    private fun clearSearchText() {
//        searchToEditText.text.clear()
//        searchFromEditText.text.clear()
    }

    private fun notifyRouteChosen(route: Route): () -> Unit {
        return {
            // Collapse the bottom sheet
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            mListener.routeChosen(route)
        }
    }
}