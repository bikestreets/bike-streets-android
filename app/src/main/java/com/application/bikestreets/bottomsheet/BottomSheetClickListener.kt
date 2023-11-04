package com.application.bikestreets.bottomsheet

import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Route

interface BottomSheetClickListener {
    fun onSettingsButtonClicked()
    fun onFollowRiderButtonClicked()
    fun showRoutes(startLocation : Location?, endLocation : Location)
    fun clearMarkers()
    fun showMarkers(startLocation: Location?, endLocation: Location)
    fun routeChosen(route: Route)
}