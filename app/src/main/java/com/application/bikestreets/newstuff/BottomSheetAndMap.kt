package com.application.bikestreets.newstuff

import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.composables.ActionButtonsContainer
import com.application.bikestreets.composables.BottomSheet
import com.application.bikestreets.composables.BottomSheetContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetAndMap(onSettingsClicked: (() -> Unit)) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

    var centerCameraTrigger by remember { mutableStateOf(false) }

    val mapboxMapController = remember { MapboxMapController() }
    var routes = remember { mutableStateOf<List<Route>>(listOf()) }

    /**
     * Sheet can either be changed due to swipe (in BottomSheet)
     * or by closing via the "X" in BottomSheetContent
     */
    fun modifySheetScaffoldState(newValue: BottomSheetValue) {
        coroutineScope.launch {
            if (newValue == BottomSheetValue.Collapsed) {
                bottomSheetScaffoldState.bottomSheetState.collapse()

                //TODO: Close keyboard if currently open
            } else {
                bottomSheetScaffoldState.bottomSheetState.expand()
            }
        }
    }

    BottomSheet(
        bottomSheetScaffoldState = bottomSheetScaffoldState,
        sheetContent = {
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
            BottomSheetContent(
                onSearchPerformed = { origin: Location?, destination: Location? ->
                    coroutineScope.launch {
                        val newRoutes = mapboxMapController.updateMapForSearch(origin, destination)
                        routes.value = newRoutes
                    }
                },
                routes = routes.value,
                notifyRouteChosen = { route -> mapboxMapController.updateMapForRoute(route) },
                bottomSheetScaffoldState = bottomSheetScaffoldState,
                onCloseClicked = { modifySheetScaffoldState(BottomSheetValue.Collapsed) }
            )
        },
        actionButtons = {
            ActionButtonsContainer(
                onSettingsButtonClicked = { onSettingsClicked() },
                onLocationButtonClicked = {
                    centerCameraTrigger = !centerCameraTrigger
                }
            )
        },
    ) { MapboxMap(mapboxMapController) }
}
