package com.application.bikestreets.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.search.common.DistanceCalculator

const val PERMISSIONS_REQUEST_LOCATION = 0

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
            Toast.makeText(context, "Failed to get location", LENGTH_SHORT).show()
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

fun requestLocationPermission(activity: Activity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        PERMISSIONS_REQUEST_LOCATION
    )
}
