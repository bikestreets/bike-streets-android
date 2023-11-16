package com.application.bikestreets.utils

import android.content.Context
import com.application.bikestreets.R
import com.application.bikestreets.api.modals.PrimitiveGeometry
import com.application.bikestreets.constants.MapLayerConstants.SELECTED_ROUTE_MAP_LAYER
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import java.io.InputStream
import java.util.Scanner

// Was unsure how to consume this directly in retrofit, so doing to conversion after the fact
fun convertToMapboxGeometry(geometry: PrimitiveGeometry): Geometry? {
    return when (geometry.type) {
        "LineString" -> {
            // Coordinates list has the structure [longitude, latitude]
            val coordinates = geometry.coordinates.map {
                Point.fromLngLat(it[0], it[1])
            }
            LineString.fromLngLats(coordinates)
        }

        else -> null
    }
}

fun createLineLayer(layerName: String): LineLayer {
    return LineLayer("$layerName-id", layerName).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND)
        .lineOpacity(1f.toDouble()).lineWidth(interpolate {
            linear()
            zoom()
            stop {
                literal(8)
                literal(0.2f.toDouble())
            }
            stop {
                literal(16)
                literal(10f.toDouble())
            }
        }).lineColor(Expression.get("stroke"))
}

fun showMapLayers(context: Context, mapStyle: Style) {
    val root = "geojson"
    val mAssetManager = context.assets

    // TODO, may be better to fetch file in the .IO thread, then render separately
    mAssetManager.list("$root/")?.forEach { fileName ->
        val featureCollection = featureCollectionFromStream(
            mAssetManager.open("$root/$fileName")
        )

        renderFeatureCollection(fileName, featureCollection, mapStyle, context)
    }

}

private fun featureCollectionFromStream(fileStream: InputStream): FeatureCollection {
    val geoJsonString = convert(fileStream)

    return FeatureCollection.fromJson(geoJsonString)
}

private fun convert(input: InputStream): String {
    val scanner = Scanner(input).useDelimiter("\\A")
    return if (scanner.hasNext()) scanner.next() else ""
}


private fun renderFeatureCollection(
    layerName: String, featureCollection: FeatureCollection, mapStyle: Style, context: Context
) {
    if (featureCollection.features() != null) {
        // add the data itself to mapStyle
        mapStyle.addSource(
            GeoJsonSource.Builder(layerName).featureCollection(featureCollection).build()
        )

        addLayerBasedOnMapType(context = context, mapStyle = mapStyle, layerName = layerName)
    }
}

// Satellite layer does not have road labels
fun addLayerBasedOnMapType(context: Context, mapStyle: Style, layerName: String) {
    if (mapTypeFromPreferences(context) == context.getString(R.string.preference_satellite)) {
        mapStyle.addLayer(createLineLayer(layerName))
    } else {
        mapStyle.addLayerBelow(createLineLayer(layerName), "road-label")
    }

}

// The best way I have found to hide a layer is just to pass in an empty route
fun hideCurrentRouteLayer(mapboxMap: MapboxMap) {
    val emptyFeatureCollection = FeatureCollection.fromFeatures(emptyList())
    val source = mapboxMap.getStyle()?.getSourceAs<GeoJsonSource>(SELECTED_ROUTE_MAP_LAYER)
    source?.featureCollection(emptyFeatureCollection)
}

