package com.application.bikestreets.composables.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.application.bikestreets.R
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.BottomSheetContentState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetContent(
    onSearchPerformed: (Location?, Location?) -> Unit,
    routes: List<Route>,
    notifyRouteChosen: (Route) -> Unit,

    // Expanding or collapsing
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    onBottomSheetScaffoldChange: (BottomSheetValue) -> Unit,

    // Showing what content on the bottom sheet
    bottomSheetContentState: BottomSheetContentState,
    onBottomSheetContentChange: (BottomSheetContentState) -> Unit,
) {
    var originLocation by remember { mutableStateOf<Location?>(null) }
    var destinationLocation by remember { mutableStateOf<Location?>(null) }

    var originSearchText by rememberSaveable { mutableStateOf("") }
    var destinationSearchText by rememberSaveable { mutableStateOf("") }

    val originSearchFocusRequester = remember { FocusRequester() }
    val destinationSearchFocusRequester = remember { FocusRequester() }

    var originSearchIsFocused by remember { mutableStateOf(false) }
    var destinationSearchIsFocused by remember { mutableStateOf(false) }
    var showRoutes by remember { mutableStateOf(false) }

    // Toggle if routes or text helper is shown
    LaunchedEffect(originSearchText) {
        showRoutes = false
    }
    LaunchedEffect(destinationSearchText) {
        showRoutes = false
    }
    LaunchedEffect(routes) {
        if (routes.isNotEmpty()) {
            // Expand to show that routes are available

            showRoutes = true
        }
    }

    // End/Destination selected by default
    fun currentlyFocusedTextField(): String {
        return if (originSearchIsFocused) {
            originSearchText
        } else {
            destinationSearchText
        }
    }

    fun defaultToCurrentLocation() {
        if (originSearchText.isEmpty()) {
            originSearchText = "Current Location"
        }
    }

    fun onSearchOptionSelected(location: Location) {
        if (currentlyFocusedTextField() == destinationSearchText) {
            destinationLocation = location
            destinationSearchText = location.name
        } else {
            originLocation = location
            originSearchText = location.name
        }
        defaultToCurrentLocation()
        onBottomSheetContentChange(BottomSheetContentState.DIRECTIONS)
        onSearchPerformed(originLocation, destinationLocation)
    }

    fun getTitleText(): String {
        return if (bottomSheetContentState == BottomSheetContentState.INITIAL) {
            "Find a Route with VAMOS"
        } else {
            "Directions"
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
    ) {
        DragIndicator()
        TopRow(
            getTitleText(),
            onCloseClicked = { onBottomSheetScaffoldChange(BottomSheetValue.Collapsed) },
            isCollapsed = bottomSheetScaffoldState.bottomSheetState.isCollapsed
        )
        if (bottomSheetContentState == BottomSheetContentState.DIRECTIONS) {
            SearchEditText(
                value = originSearchText,
                onValueChange = { value -> originSearchText = value },
                hint = stringResource(id = R.string.search_set_origin),
                modifier = Modifier
                    .focusRequester(destinationSearchFocusRequester)
                    .onFocusChanged { focusState ->
                        if(focusState.isFocused){
                            originSearchIsFocused = focusState.isFocused
                            onBottomSheetScaffoldChange(BottomSheetValue.Expanded)
                        }
                    }
            )
        }
        SearchEditText(
            value = destinationSearchText,
            onValueChange = { value -> destinationSearchText = value },
            hint = stringResource(id = R.string.search_set_destination),
            modifier = Modifier
                .focusRequester(destinationSearchFocusRequester)
                .onFocusChanged { focusState ->
                    if(focusState.isFocused) {
                        destinationSearchIsFocused = focusState.isFocused
                        onBottomSheetScaffoldChange(BottomSheetValue.Expanded)
                    }
                }
        )

        if (!showRoutes) {
            SearchOptions(
                modifier = Modifier.fillMaxWidth(),
                onSearchOptionSelected = { location -> onSearchOptionSelected(location) },
                newSearchQuery = currentlyFocusedTextField(),
            )
        } else {
            routes.forEachIndexed { index, route ->
                RouteOption(
                    index = index + 1,
                    distance = route.distance,
                    onGoClicked = { notifyRouteChosen(route) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
fun BottomSheetUiPreview() {
    BottomSheetContent(
        onSearchPerformed = { _, _ -> },
        routes = listOf(),
        notifyRouteChosen = {},
        bottomSheetScaffoldState = BottomSheetScaffoldState(
            drawerState = DrawerState(
                DrawerValue.Closed
            ),
            BottomSheetState(BottomSheetValue.Expanded),
            SnackbarHostState()
        ),
        onBottomSheetScaffoldChange = { _ -> },
        bottomSheetContentState = BottomSheetContentState.DIRECTIONS,
        onBottomSheetContentChange = { _ -> }
    )
}