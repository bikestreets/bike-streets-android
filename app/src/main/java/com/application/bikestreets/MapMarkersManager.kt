package com.application.bikestreets

import android.content.Context
import androidx.core.content.ContextCompat
import com.application.bikestreets.utils.moveCamera
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager


class MapMarkersManager(mapView: MapView) {

    private val mapboxMap = mapView.getMapboxMap()
    private val circleAnnotationManager =
        mapView.annotations.createCircleAnnotationManager(null)
    private val markers = mutableMapOf<Long, Point>()

    private var onMarkersChangeListener: (() -> Unit)? = null

    val hasMarkers: Boolean
        get() = markers.isNotEmpty()

    fun clearMarkers() {
        markers.clear()
        circleAnnotationManager.deleteAll()
    }

    fun showMarker(destination: Point, start: Point, context: Context) {
        clearMarkers()

        val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
            .withPoint(destination)
            .withCircleRadius(8.0)
            .withCircleColor(ContextCompat.getColor(context, R.color.destination_pin))
            .withCircleStrokeWidth(3.0)
            .withCircleStrokeColor("#ffffff")

        val annotation = circleAnnotationManager.create(circleAnnotationOptions)
        markers[annotation.id] = destination


        val coordinates = listOf(start, destination)
        val cameraOptions = mapboxMap.cameraForCoordinates(
            coordinates, MARKERS_INSETS, bearing = null, pitch = null
        )

        moveCamera(map = mapboxMap, cameraOptions = cameraOptions)

        onMarkersChangeListener?.invoke()
    }

    private companion object {

        val MARKERS_EDGE_OFFSET = Utils.dpToPx(64F).toDouble()

        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET
        )
    }
}