package com.application.bikestreets.newstuff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.composables.ActionButtonsContainer
import com.application.bikestreets.composables.BottomSheet
import com.application.bikestreets.composables.BottomSheetContent
import com.application.bikestreets.composables.ExpandedType

@Composable
fun BottomSheetAndMap(onSettingsClicked: (() -> Unit)) {

    var expandedState by remember {
        mutableStateOf(ExpandedType.COLLAPSED)
    }

    /**
     * Sheet can either be changed due to swipe (in BottomSheet)
     * or by closing via the "X" in BottomSheetContent
     */
    fun modifySheetExpandState(expandState: ExpandedType) {
        expandedState = expandState
    }

    BottomSheet(
        expandedState = expandedState,
        onExpandedTypeChange = { expandState -> modifySheetExpandState(expandState) },
        sheetContent = {
            MapboxMap()
//            BottomSheetContent(
//                onSearchPerformed = { origin: Location?, destination: Location? ->
//                    onSearchOptionSelected(
//                        origin,
//                        destination
//                    )
//                },
//                routes = routes,
//                notifyRouteChosen = { route -> notifyRouteChosen(route) },
//                onCloseClicked = { closeBottomSheet() }
//            )
        },
        actionButtons = {
            MapboxMap()
//            ActionButtonsContainer(
//                onSettingsButtonClicked = { onSettingsClicked },
//                onLocationButtonClicked = { onLocationButtonClicked() }
//            )
        },
        content = { MapboxMap() },
    )
}