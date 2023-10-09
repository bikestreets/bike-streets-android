package com.application.bikestreets.utils

import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo

/** Can pass in either a single point to center the map,
 *  or
 *  (optional) pass in a camera option that includes multiple points
 */
fun moveCamera(map: MapboxMap, location: Point? = null, cameraOptions: CameraOptions? = null) {
    map.easeTo(
        cameraOptions = cameraOptions ?: defaultCameraOptions(location),
        animationOptions = MapAnimationOptions.mapAnimationOptions {
            duration(500)
            interpolator(FastOutSlowInInterpolator())
        }
    )
}

private fun defaultCameraOptions(location: Point?): CameraOptions {
    return CameraOptions.Builder()
        .center(location)
        .build()
}