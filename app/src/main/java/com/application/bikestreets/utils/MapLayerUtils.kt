package com.application.bikestreets.utils

import com.application.bikestreets.MainActivity
import com.application.bikestreets.StringToStream
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
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import java.io.InputStream

// Was unsure how to consume this directly in retrofit, so doing to conversion after the fact
fun convertToMapboxGeometry(geometry: PrimitiveGeometry): Geometry? {
    return when (geometry.type) {
        "LineString" -> {
            // Assuming your coordinates list has the structure [longitude, latitude]
            val coordinates = geometry.coordinates.map {
                Point.fromLngLat(it[0], it[1])
            }
            LineString.fromLngLats(coordinates)
        }
        // Handle other geometry types similarly
        else -> null
    }
}

fun createLineLayer(layerName: String): LineLayer {
//    val lineColor = colorForLayer(layerName)

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

//TODO: Do something with this layer names, clean up to use more shared logic
//private fun colorForLayer(layerName: String): Int {
//    // This is lazy coupling and will break, but I want to see it work as a proof-of-concept.
//    // A more flexible refactor involves inspecting the GeoJson file itself to get the layer
//    // name, then matching the color based on that (or we can save the layer color as metadata.)
//    val lineColor = when (layerName) {
//        "terms_of_use.txt" -> R.color.mapTrails
//        "1-bikestreets-master-v0.3.geojson" -> R.color.mapBikeStreets
//        "2-trails-master-v0.3.geojson" -> R.color.mapTrails
//        "3-bikelanes-master-v0.3.geojson" -> R.color.mapBikeLane
//        "4-bikesidewalks-master-v0.3.geojson" -> R.color.mapRideSidewalk
//        "5-walk-master-v0.3.geojson" -> R.color.mapWalkSidewalk
//        MapLayerConstants.PUSHING_BIKE_LAYER -> R.color.sidewalk_segment
//        else -> R.color.mapDefault
//    }
//
//    // convert line color from R.color format to a more standard color format that
//    // PropertyFactory.lineColor knows how to work with
//    return ContextCompat.getColor(this, lineColor)
//}


fun showMapLayers(activity: MainActivity, mapStyle: Style) {
    val root = "geojson"
    val mAssetManager = activity.assets

    mAssetManager.list("$root/")?.forEach { fileName ->
        val featureCollection = featureCollectionFromStream(
            mAssetManager.open("$root/$fileName")
        )

        renderFeatureCollection(fileName, featureCollection, mapStyle)
    }

}

private fun featureCollectionFromStream(fileStream: InputStream): FeatureCollection {
    val geoJsonString = StringToStream.convert(fileStream)

    return FeatureCollection.fromJson(geoJsonString)
}

private fun renderFeatureCollection(
    layerName: String, featureCollection: FeatureCollection, mapStyle: Style
) {
    if (featureCollection.features() != null) {
        // add the data itself to mapStyle
        mapStyle.addSource(
            GeoJsonSource.Builder(layerName).featureCollection(featureCollection).build()
        )

//        if (mapTypeFromPreferences().equals(getString(R.string.preference_satellite))) {
//            //TODO: In satellite view, routes appear above the navigation line
//            mapStyle.addLayer(createLineLayer(layerName))
//        } else {
        // create a line layer that reads the GeoJSON data that we just added
//            mapStyle.addLayerBelow(createLineLayer(layerName), "road-label")
        mapStyle.addLayer(createLineLayer(layerName))
//        }

    }
}

// The best way I have found to hide a layer is just to pass in an empty route
fun hideCurrentRouteLayer(mapboxMap: MapboxMap) {
    val emptyFeatureCollection = FeatureCollection.fromFeatures(emptyList())
    val source = mapboxMap.getStyle()?.getSourceAs<GeoJsonSource>(SELECTED_ROUTE_MAP_LAYER)
    source?.featureCollection(emptyFeatureCollection)
}

