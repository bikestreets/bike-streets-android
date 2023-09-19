package com.application.bikestreets.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions

// Convert from List<List<Double>> to List<Point> for use with mapbox functions
fun convertToListOfPoints(coordinateList: List<List<Double>>): List<Point> =
    coordinateList.map { coordinate ->
        Point.fromLngLat(coordinate[0], coordinate[1])
    }


fun drawPolylineSegment(
    coordinateList: List<List<Double>>,
    lineColor: Int,
    polylineAnnotationManager: PolylineAnnotationManager,
    context: Context
) {
    val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
        .withPoints(convertToListOfPoints(coordinateList))
        .withLineColor(ContextCompat.getColor(context, lineColor))
        .withLineJoin(LineJoin.ROUND)
        .withLineWidth(6.0)

    // Add the resulting polygon to the map.
    polylineAnnotationManager.create(polylineAnnotationOptions)

}
