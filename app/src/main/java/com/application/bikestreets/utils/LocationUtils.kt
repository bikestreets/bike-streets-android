package com.application.bikestreets.utils

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.search.common.DistanceCalculator

@SuppressLint("MissingPermission")
fun LocationEngine.lastKnownLocation(context: Context, callback: (Point?) -> Unit) {
    if (!PermissionsManager.areLocationPermissionsGranted(context)) {
        callback(null)
    }

    getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            val location =
                (result?.locations?.lastOrNull() ?: result?.lastLocation)?.let { location ->
                    Point.fromLngLat(location.longitude, location.latitude)
                }
            callback(location)
        }

        override fun onFailure(exception: Exception) {
            callback(null)
        }
    })
}

fun LocationEngine.userDistanceTo(
    context: Context,
    destination: Point,
    callback: (Double?) -> Unit
) {
    lastKnownLocation(context) { location ->
        if (location == null) {
            callback(null)
        } else {
            val distance = DistanceCalculator.instance(latitude = location.latitude())
                .distance(location, destination)
            callback(distance)
        }
    }
}
