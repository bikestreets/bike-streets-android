package com.application.bikestreets.bottomsheet

import com.application.bikestreets.api.modals.Location

interface BottomSheetClickListener {
    fun onSettingsButtonClicked()
    fun onFollowRiderButtonClicked()
    fun showRoutes(startLocation : Location?, endLocation : Location)
    fun clearMarkers()
    fun showMarkers(startLocation: Location?, endLocation: Location)
}