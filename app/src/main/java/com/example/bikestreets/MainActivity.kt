package com.example.bikestreets

// used to handle geojson loading
import java.io.InputStream
import android.content.res.AssetManager

// used to handle geojson map layer drawing
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import android.graphics.Color
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

import java.util.*

import android.widget.ImageView
import com.mapbox.mapboxsdk.location.LocationComponent

class MainActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    private var permissionsManager: PermissionsManager ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, "pk.eyJ1Ijoianpvcm5vdyIsImEiOiJjazVsOWhkc2YwbWgwM2xuNXJvdnlhN2o3In0.tW5TbWlDY-ciFrYSv6qTOA")

        setContentView(R.layout.activity_main)

        // keep the device from falling asleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // Map is set up and the style has loaded. Now you can add data or make other map adjustments
                showDeviceLocation(mapboxMap, it)

                // add geojson layers
                showMapLayers(this, it)

                // recenter item
                val btn_click_me = findViewById<ImageView>(R.id.follow_rider)
                btn_click_me.setOnClickListener {
                    // your code to perform when the user clicks on the button
                    setCameraMode(mapboxMap.locationComponent)
                }
            }
        }

        if (PermissionsManager.areLocationPermissionsGranted(this)){
            // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location
        } else {
            var permissionsListener: PermissionsListener = object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                    // provides explanation of why permission is required
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location


                    } else {
                        // User denied the permission
                    }
                }
            }

            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    private fun setCameraMode(locationComponent: LocationComponent ){
        locationComponent?.setCameraMode(CameraMode.TRACKING, 10, 17.0, null, null, null)
    }

    private fun showMapLayers(activity: MainActivity, mapStyle: Style) {
        var mAssetManager: AssetManager = activity.getAssets()
        val root: String = "geojson"

        mAssetManager.list("$root/").forEach { fileName ->
            var featureCollection = featureCollectionFromStream(
                mAssetManager.open("$root/$fileName")
            )

            renderFeatureCollection(fileName, featureCollection, mapStyle)
        }

    }

    private fun featureCollectionFromStream(fileStream: InputStream): FeatureCollection {
        var geoJsonString = convertStreamToString(fileStream)

        return FeatureCollection.fromJson(geoJsonString)
    }

    private fun colorForLayer(layerName: String): Int {
        // This is lazy coupling and will break, but I want to see it work as a proof-of-concept.
        // A more flexible refactor involves inspecting the GeoJson file itself to get the layer
        // name, then matching the color based on that (or we can save the layer color as metadata.)
        val hexColor = when(layerName) {
            "1-bikestreets-master-v0.3.geojson" -> "#0000FF"
            "3-bikelanes-master-v0.3.geojson" -> "#000000"
            "5-walk-master-v0.3.geojson" -> "#FF0000"
            "2-trails-master-v0.3.geojson" -> "#008000"
            "4-bikesidewalks-master-v0.3.geojson" -> "#FF0000"
            else -> "#000000"
        }

        return Color.parseColor(hexColor)
    }

    private fun createLineLayer(layerName: String): LineLayer {
        val lineColor = colorForLayer(layerName)

        return LineLayer("$layerName-id", layerName)
            .withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(.7f),
                PropertyFactory.lineWidth(7f),
                PropertyFactory.lineColor(lineColor))
    }

    private fun renderFeatureCollection(layerName: String, featureCollection: FeatureCollection, mapStyle: Style) {
        if(featureCollection.features() != null) {
            // add the data itself to mapStyle
            mapStyle.addSource(GeoJsonSource(layerName, featureCollection))

            // create a line layer that reads the GeoJSON data that we just added
            mapStyle.addLayer(createLineLayer(layerName))
        }
    }

    private fun convertStreamToString(input: InputStream): String {
        val scanner = Scanner(input).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    private fun showDeviceLocation(mapboxMap: MapboxMap, style: Style) {

        val locationComponentOptions = LocationComponentOptions
            .builder(this)
            .build()

        val locationComponentActivationOptions = LocationComponentActivationOptions
            .builder(this, style)
            .locationComponentOptions(locationComponentOptions)
            .build()

        var locationComponent = mapboxMap.locationComponent

        // Activate with options
        locationComponent.activateLocationComponent(locationComponentActivationOptions)

        // Enable to make component visible
        locationComponent.setLocationComponentEnabled(true)

        // Set the component's camera mode
        setCameraMode(mapboxMap.locationComponent)
        // Set the component's render mode
        locationComponent.setRenderMode(RenderMode.COMPASS)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
}

