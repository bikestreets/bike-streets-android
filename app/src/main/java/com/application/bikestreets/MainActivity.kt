package com.application.bikestreets

// used to handle geojson loading
import android.content.Intent
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
import android.preference.PreferenceManager

// Views Components
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private val activity: MainActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, "pk.eyJ1Ijoianpvcm5vdyIsImEiOiJjazVsOWhkc2YwbWgwM2xuNXJvdnlhN2o3In0.tW5TbWlDY-ciFrYSv6qTOA")

        setContentView(R.layout.activity_main)

        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        // enable settings button
        enableSettingsButton()

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap -> setupMapboxMap(mapboxMap) }
    }

    private fun setScreenModeFromPreferences() {
        // extract string from strings.xml file (as integer key) and convert to string
        val keepScreenOnKey = getResources().getString(R.string.keep_screen_preference_key)

        // extracted stored value for user's screen mode preference
        val keepScreenOnPreference = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean(keepScreenOnKey, true) // default is to keep the screen on

        // adding this flag will keep the screen from turning off if the app goes idle
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (keepScreenOnPreference) {
            window.addFlags(keepScreenOnFlag)
        } else {
            window.clearFlags(keepScreenOnFlag)
        }
    }

    private fun launchTermsOfUse() {
        val intent = Intent(this, TermsOfUse::class.java).apply {}
        // start Terms Of Use activity regardless of whether or not they have any to sign. Don't
        // worry: it will bail right away if it decides that the user is up-to-date
        startActivity(intent)
    }

    private fun enableSettingsButton() {
        // get the button
        val settingsButton = findViewById<ImageView>(R.id.settings)

        // show the button
        settingsButton.visibility = View.VISIBLE

        // enable the button's functionality
        settingsButton.setOnClickListener {
            val intent = Intent(this, About::class.java).apply {}
            startActivity(intent)
        }
    }

    private fun enableFollowRiderButton(mapboxMap: MapboxMap) {
        // get the button
        val followRiderButton = findViewById<ImageView>(R.id.follow_rider)

        // show the button
        followRiderButton.visibility = View.VISIBLE

        // enable the button's functionality
        followRiderButton.setOnClickListener {
            setCameraMode(mapboxMap.locationComponent, cameraModeFromPreferences())
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

    // The cameraMode Int references one of the CameraMode enums, e.g. CameraMode.TRACKING
    private fun setCameraMode(locationComponent: LocationComponent, cameraMode: Int){
        locationComponent?.setCameraMode(
            cameraMode,
            10,
            17.0, 
            null,
            null,
            null
        )
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
        val lineColor = when(layerName) {
            "terms_of_use.txt" -> R.color.mapTrails
            "1-bikestreets-master-v0.3.geojson" -> R.color.mapBikeStreets
            "2-trails-master-v0.3.geojson" -> R.color.mapTrails
            "3-bikelanes-master-v0.3.geojson" -> R.color.mapBikeLane
            "4-bikesidewalks-master-v0.3.geojson" -> R.color.mapRideSidewalk
            "5-walk-master-v0.3.geojson" -> R.color.mapWalkSidewalk
            else -> R.color.mapDefault
        }

        // convert line color from R.color format to a more standard color format that
        // PropertyFactory.lineColor knows how to work with
        return ContextCompat.getColor(this, lineColor)
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

    private fun cameraModeFromPreferences(): Int {
        // extract string from strings.xml file (as integer key) and convert to string
        val orientataionPreferenceKey = getResources().getString(R.string.map_orientation_preference_key)

        // use key to extract saved camera mode preference string. Default to tracking compass,
        // a.k.a. "Direction of Travel"
        val orientationPreferenceString = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString(orientataionPreferenceKey, "direction_of_travel")

       // convert into a MapBox camera mode and return
        return if (orientationPreferenceString == "fixed") {
            CameraMode.TRACKING
        } else {
            CameraMode.TRACKING_COMPASS
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

        // now that we have the user's preference set the map to use that camera mode
        setCameraMode(mapboxMap.locationComponent, cameraModeFromPreferences())

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

        // if the user is returning from the settings page, those settings will need to be applied
        mapView?.getMapAsync { mapboxMap -> setupMapboxMap(mapboxMap) }

        // reload the autosleep preference in case it was changed while the activity was paused
        setScreenModeFromPreferences()
    }

    private fun mapTypeFromPreferences(): String {
        // extract preference key string from strings.xml
        val mapTypePreferenceKey = getResources().getString(R.string.map_type_preference_key)

        // use that key to extract the stored user preference
        return PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString(mapTypePreferenceKey, "street_map")
    }

    private fun addRoutesAndLocation(mapboxMap: MapboxMap, style: Style) {
        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        showDeviceLocation(mapboxMap, style)

        // add geojson layers
        showMapLayers(this, style)
    }

    private fun setupMapboxMap(mapboxMap: MapboxMap) {
        // default the map to a zoomed in view of the city. Note that this is overriden by
        // showDeviceLocation below if location services are enabled and permitted
        centerMapDefault(mapboxMap)

        // apply map style conditionally, based on user's preferences.
        if (mapTypeFromPreferences() == "satellite_view") {
            mapboxMap.setStyle(Style.SATELLITE) { addRoutesAndLocation(mapboxMap, it) }
        } else {
            // pull custom street map styling from json source
            val customStyles = Style.Builder().fromUri("asset://stylejson/style.json")
            mapboxMap.setStyle(customStyles) { addRoutesAndLocation(mapboxMap, it) }
        }

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

