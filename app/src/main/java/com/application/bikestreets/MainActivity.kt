package com.application.bikestreets

// used to handle geojson loading
import android.content.Context
import java.io.InputStream

// used to handle geojson map layer drawing
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import android.graphics.Color
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

import android.os.Bundle

// Views Components
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.ImageView
import android.widget.TextView

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

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent

class MainActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    private var permissionsManager: PermissionsManager ?= null
    private var followRiderButton: ImageView ?= null
    private val activity: MainActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, "pk.eyJ1Ijoianpvcm5vdyIsImEiOiJjazVsOWhkc2YwbWgwM2xuNXJvdnlhN2o3In0.tW5TbWlDY-ciFrYSv6qTOA")

        setContentView(R.layout.activity_main)

        // keep the device from falling asleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // launch terms of use if unsigned
        launchTermsOfUse()

        // save off recentering button reference into global so that it can be used later
        followRiderButton = findViewById<ImageView>(R.id.follow_rider)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.Builder().fromUri("asset://stylejson/style.json")) {
                // default the map to a zoomed in view of the city
                centerMapDefault(mapboxMap)

                // Map is set up and the style has loaded. Now you can add data or make other map adjustments
                showDeviceLocation(mapboxMap, it)

                // add geojson layers
                showMapLayers(this, it)
            }
        }
    }

    private fun launchTermsOfUse() {
        val termsManager = TermsOfUseManager(this)

        // check of unsigned Terms of Use
        if (termsManager.hasUnsignedTermsOfUse()) {
            // fill terms of use window
            val termsOfUseWindow = findViewById<TextView>(R.id.terms_of_use)
            termsOfUseWindow.text = termsManager.contractText()

            // activate the accept button
            val termsOfUseAcceptButton = findViewById<Button>(R.id.terms_of_use_accept)
            termsOfUseAcceptButton.setOnClickListener {
                termsManager.accept()

                // hide the terms of use window
                findViewById<ScrollView>(R.id.terms_of_use_window).visibility = View.GONE
            }

            // now that it's ready, show the overlay
            findViewById<ScrollView>(R.id.terms_of_use_window).visibility = View.VISIBLE
        }
    }

    private fun enableFollowRiderButton(mapboxMap: MapboxMap) {
        // show the button
        followRiderButton?.setVisibility(View.VISIBLE)

        // enable the button's functionality
        followRiderButton?.setOnClickListener {
            setCameraMode(mapboxMap.locationComponent)
        }

    }

    private fun centerMapDefault(mapboxMap: MapboxMap) {
        val position = CameraPosition.Builder()
            .target(LatLng(39.7326381,-104.9687837))
            .zoom(12.0)
            .tilt(0.0)
            .build()

        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)

        mapboxMap.moveCamera(cameraUpdate)
    }

    private fun showDeviceLocation(mapboxMap: MapboxMap, style: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(activity)){
            enableFollowRiderButton(mapboxMap)
            drawLocationOnMap(mapboxMap, style)
        } else {
            var permissionsListener: PermissionsListener = object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) { }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        enableFollowRiderButton(mapboxMap)
                        drawLocationOnMap(mapboxMap, style)
                    } else {
                        // User denied the permission: don't put a point on the map at all
                    }
                }
            }

            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager?.requestLocationPermissions(activity)
        }
    }

    private fun setCameraMode(locationComponent: LocationComponent ){
        locationComponent?.setCameraMode(CameraMode.TRACKING, 10, 17.0, null, null, null)
    }

    private fun showMapLayers(activity: MainActivity, mapStyle: Style) {
        val root: String = "geojson"
        val mAssetManager = activity.assets

        mAssetManager.list("$root/").forEach { fileName ->
            var featureCollection = featureCollectionFromStream(
                mAssetManager.open("$root/$fileName")
            )

            renderFeatureCollection(fileName, featureCollection, mapStyle)
        }

    }

    private fun featureCollectionFromStream(fileStream: InputStream): FeatureCollection {
        var geoJsonString = StringToStream.convert(fileStream)

        return FeatureCollection.fromJson(geoJsonString)
    }

    private fun colorForLayer(layerName: String): Int {
        // This is lazy coupling and will break, but I want to see it work as a proof-of-concept.
        // A more flexible refactor involves inspecting the GeoJson file itself to get the layer
        // name, then matching the color based on that (or we can save the layer color as metadata.)
        val hexColor = when(layerName) {
            "terms_of_use.txt" -> "#0000FF"
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
                PropertyFactory.lineWidth(interpolate(linear(), zoom(),
                    stop(8, .2f),
                    stop(16, 10f))),
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


    private fun drawLocationOnMap(mapboxMap: MapboxMap, style: Style) {
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

