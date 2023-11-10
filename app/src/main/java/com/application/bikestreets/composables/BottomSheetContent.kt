package com.application.bikestreets.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.application.bikestreets.R
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.bottomsheet.BottomSheetStates

@Composable
fun BottomSheetContent(
    onSearchPerformed: ((Location?, Location?) -> Unit),
    routes: List<Route>,
    notifyRouteChosen: ((Route) -> Unit),
) {
    var originLocation by remember { mutableStateOf<Location?>(null) }
    var destinationLocation by remember { mutableStateOf<Location?>(null) }

    var originSearchText by rememberSaveable { mutableStateOf("") }
    var destinationSearchText by rememberSaveable { mutableStateOf("") }

    val originSearchFocusRequester = remember { FocusRequester() }
    val destinationSearchFocusRequester = remember { FocusRequester() }

    var originSearchIsFocused by remember { mutableStateOf(false) }
    var destinationSearchIsFocused by remember { mutableStateOf(false) }

    val bottomSheetState by remember { mutableStateOf(BottomSheetStates.INITIAL) }


    // End/Destination selected by default
    fun currentlyFocusedTextField(): String {
        return if (originSearchIsFocused) {
            originSearchText
        } else {
            destinationSearchText
        }
    }

    fun onSearchOptionSelected(location: Location) {
        if (currentlyFocusedTextField() == destinationSearchText) {
            destinationLocation = location
            destinationSearchText.apply { location.name }
        } else {
            originLocation = location
            originSearchText.apply { location.name }
        }
        bottomSheetState.apply { BottomSheetStates.DIRECTIONS }
        onSearchPerformed(originLocation, destinationLocation)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.default_margin))
    ) {
        DragIndicator()
        if (bottomSheetState == BottomSheetStates.DIRECTIONS) {
            SearchEditText(
                value = originSearchText,
                onValueChange = { value -> originSearchText = value },
                hint = stringResource(id = R.string.search_set_origin),
                modifier = Modifier
                    .focusRequester(destinationSearchFocusRequester)
                    .onFocusChanged { focusState ->
                        originSearchIsFocused = focusState.isFocused
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
                    destinationSearchIsFocused = focusState.isFocused
                }
        )

        if (routes.isEmpty()) {
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

@Preview(showBackground = true)
@Composable
fun BottomSheetUiPreview() {
    BottomSheetContent(
        onSearchPerformed = { _, _ -> {} },
        routes = listOf(),
        notifyRouteChosen = {},
    )
}