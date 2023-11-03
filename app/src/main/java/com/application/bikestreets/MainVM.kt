package com.application.bikestreets

import android.content.Context
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.bikestreets.utils.mapTypeFromPreferences
import com.application.bikestreets.utils.showMapLayers
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import kotlinx.coroutines.launch

class MainVM : ViewModel() {
    fun clearText() {
        TODO("Not yet implemented")
    }

    fun enableFollowRiderButton() {

    }

    fun loadMapboxStyle(mapboxMap: MapboxMap, context: Context) {
        viewModelScope.launch {
            var mapStyle = "asset://stylejson/style.json"

            // apply map style conditionally, based on user's preferences.
            if (mapTypeFromPreferences(context).equals(
                    getString(
                        context,
                        R.string.preference_satellite
                    )
                )
            ) {
                mapStyle = Style.SATELLITE
            }

            // Load style, on compete show layers

            mapboxMap.loadStyleUri(mapStyle) { showMapLayers(context, it) }
        }
    }

}