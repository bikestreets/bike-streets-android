package com.application.bikestreets

import android.content.Context
import android.util.Log
import com.application.bikestreets.api.RoutingService
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.api.modals.Mode
import com.application.bikestreets.api.modals.Route
import com.application.bikestreets.constants.MapLayerConstants
import com.application.bikestreets.theme.Colors
import com.application.bikestreets.utils.addLayerBasedOnMapType
import com.application.bikestreets.utils.convertToMapboxGeometry
import com.application.bikestreets.utils.getColorHexString
import com.application.bikestreets.utils.moveCamera
import com.application.bikestreets.utils.showToast
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles the bridge between actions taken on the bottom sheet
 * and what changes need to be made on the map
 */
class MapboxMapController {
    private lateinit var mMapView : MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var mContext: Context
    private lateinit var location: Point
    private lateinit var mapMarkersManager: MapMarkersManager


    fun attachMapboxMap(mapView: MapView, map: MapboxMap, context: Context) {
        mMapView = mapView
        mapboxMap = map
        mContext = context

        // Load Map Markers
        mapMarkersManager = MapMarkersManager(mapView)

    }

    fun loadLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(mContext)) {
            mMapView.location.updateSettings {
                enabled = true
            }

            mMapView.location.addOnIndicatorPositionChangedListener(object :
                OnIndicatorPositionChangedListener {
                override fun onIndicatorPositionChanged(point: Point) {
                    location = point
                    moveCamera(map = mMapView.getMapboxMap(), location = location)

                    mMapView.location.removeOnIndicatorPositionChangedListener(this)
                }
            })
        } else {
            // Location not enabled, move camera to a default location (No animation)
            val cameraPosition = CameraOptions.Builder()
                .zoom(12.0)
                .center(Point.fromLngLat(-104.9687837, 39.7326381))
                .build()
            // set camera position
            mMapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    suspend fun updateMapForSearch(origin: Location?, destination: Location?): List<Route> {
        var routes = listOf<Route>()

        if (isPossibleRoute(origin) && destination != null) {
            val startCoordinates = origin?.coordinate ?: location

            withContext(Dispatchers.Main) {
                try {
                    val routingService = RoutingService()
                    val routingDirections = routingService.getRoutingDirections(
                        startCoordinates = startCoordinates,
                        endCoordinates = destination.coordinate,
                    )

                    // Show markers on map
                    mapMarkersManager.showMarker(
                        destination = destination.coordinate,
                        start = startCoordinates,
                    )

                    // Render Route line on map
                    if (routingDirections?.routes != null) {
                        displayRoutesOnMap(routingDirections.routes)
                        routes = routingDirections.routes
                    } else {
                        // Do Nothing
                    }
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Navigation error: $e")
                }
            }
        } else {
            showToast(mContext, "Location is not set, cannot show route")
        }
        return routes
    }


    private fun displayRoutesOnMap(routes: List<Route>?) {

        val mapStyle = mapboxMap?.getStyle()

        val selectedRouteGeometry: MutableList<Feature> = mutableListOf()

        routes?.forEach {
            val legs = it.legs
            val steps = legs.flatMap { leg -> leg.steps }
            steps.forEach { step ->
                if (Mode.getMode(step.mode) == Mode.PUSHING_BIKE) {

                    val mapBoxGeometry = convertToMapboxGeometry(step.geometry)
                    val properties = JsonObject()
                    properties.addProperty(
                        "stroke",
                        getColorHexString(Colors.sidewalk)
                    )

                    selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
                } else {
                    val mapBoxGeometry = convertToMapboxGeometry(step.geometry)
                    val properties = JsonObject()
                    properties.addProperty(
                        "stroke",
                        getColorHexString(Colors.vamosLightBlue)
                    )

                    selectedRouteGeometry.add(Feature.fromGeometry(mapBoxGeometry, properties))
                }
            }
        }


        val pushingFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(selectedRouteGeometry)


        /** Once a layer is added, we cannot delete and re-render it,
         *  instead we keep it hidden or override the route segment as needed
         *
         *  This should be done with a Polyline Annotation, but there currently isn't multicolored
         *  line support unless using a gradient
         */
        val layerSource =
            mapboxMap?.getStyle()
                ?.getSourceAs<GeoJsonSource>(MapLayerConstants.SELECTED_ROUTE_MAP_LAYER)

        if (layerSource == null) {
            mapStyle?.addSource(
                GeoJsonSource.Builder(MapLayerConstants.SELECTED_ROUTE_MAP_LAYER)
                    .featureCollection(pushingFeatureCollection).build()
            )

            // Add layer above rendered routes
            mapStyle?.let {
                addLayerBasedOnMapType(
                    mContext,
                    it,
                    MapLayerConstants.SELECTED_ROUTE_MAP_LAYER
                )
            }

        } else {
            layerSource.featureCollection(pushingFeatureCollection)
        }
    }

    private fun isPossibleRoute(startLocation: Location?): Boolean {
        // TODO: Refine this when location is turned off
        return startLocation != null || ::location.isInitialized
    }


    /**
     * Preform a camera update. Return false if unable
     */
    fun centerOnCurrentLocation(): Boolean {
        return if (PermissionsManager.areLocationPermissionsGranted(mContext)) {
            if(::location.isInitialized){
                mapboxMap?.let { moveCamera(it, location) }
            } else {
                loadLocation()
            }
            true
        } else {
            false
        }
    }
}