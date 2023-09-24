package com.application.bikestreets

import android.content.Context
import androidx.core.content.ContextCompat
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
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

    var onMarkersChangeListener: (() -> Unit)? = null

    val hasMarkers: Boolean
        get() = markers.isNotEmpty()

    fun clearMarkers() {
        markers.clear()
        circleAnnotationManager.deleteAll()
    }

    fun showMarker(coordinate: Point, context: Context) {
        showMarkers(listOf(coordinate), context)
    }

    fun showMarkers(coordinates: List<Point>, context: Context) {
        clearMarkers()
        if (coordinates.isEmpty()) {
            onMarkersChangeListener?.invoke()
            return
        }

        coordinates.forEach { coordinate ->
            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(coordinate)
                .withCircleRadius(8.0)
                .withCircleColor(ContextCompat.getColor(context, R.color.destination_pin))
                .withCircleStrokeWidth(3.0)
                .withCircleStrokeColor("#ffffff")

            val annotation = circleAnnotationManager.create(circleAnnotationOptions)
            markers[annotation.id] = coordinate
        }

        if (coordinates.size == 1) {
            CameraOptions.Builder()
                .center(coordinates.first())
                .padding(MARKERS_INSETS_OPEN_CARD)
                .zoom(14.0)
                .build()
        } else {
            mapboxMap.cameraForCoordinates(
                coordinates, MARKERS_INSETS, bearing = null, pitch = null
            )
        }.also {
            mapboxMap.setCamera(it)
        }
        onMarkersChangeListener?.invoke()
    }

    private companion object {

        val MARKERS_EDGE_OFFSET = Utils.dpToPx(64F).toDouble()
        val PLACE_CARD_HEIGHT = Utils.dpToPx(300F).toDouble()

        //TODO Adjust camera so both start and end are visible
        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET
        )

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )
    }
}