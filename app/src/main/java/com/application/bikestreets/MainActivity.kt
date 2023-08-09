package com.application.bikestreets

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.application.bikestreets.databinding.ActivityMainBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    private var permissionsManager: PermissionsManager? = null
    private val activity: MainActivity = this

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        setScreenModeFromPreferences()

        // launch terms of use if unsigned
        launchTermsOfUse()

        // enable settings button
        enableSettingsButton()

        mapView = binding.mapView
        setupMapboxMap()
    }

    private fun setScreenModeFromPreferences() {
        // extract string from strings.xml file (as integer key) and convert to string
        val keepScreenOnKey = getResources().getString(R.string.keep_screen_preference_key)

        // extracted stored value for user's screen mode preference
        // TODO: remove depreciated code
        val keepScreenOnPreference = PreferenceManager.getDefaultSharedPreferences(this)
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
        val settingsButton = binding.settings

        // show the button
        settingsButton.visibility = View.VISIBLE

        // enable the button's functionality
        settingsButton.setOnClickListener {
            val intent = Intent(this, About::class.java).apply {}
            startActivity(intent)
        }
    }

    private fun enableFollowRiderButton() {
        //TODO: fix this
//        // get the button
//        val followRiderButton = binding.followRider
//
//        // show the button
//        followRiderButton.visibility = View.VISIBLE
//
//        // enable the button's functionality
//        followRiderButton.setOnClickListener {
//            val locationComponent = mapView?.location
//            mapView?.getMapboxMap()?.setCamera(locationComponent)
//        }
    }

    private fun checkLocationPermission(onMapReady: () -> Unit) {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            onMapReady()
        } else {
            val permissionsListener: PermissionsListener = object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        onMapReady()
                    } else {
                        // User denied the permission: don't put a point on the map at all
                        activity.finish()
                    }
                }
            }

            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager?.requestLocationPermissions(activity)
        }
    }

    private fun showMapLayers(activity: MainActivity, mapStyle: Style) {
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

    private fun colorForLayer(layerName: String): Int {
        // This is lazy coupling and will break, but I want to see it work as a proof-of-concept.
        // A more flexible refactor involves inspecting the GeoJson file itself to get the layer
        // name, then matching the color based on that (or we can save the layer color as metadata.)
        val lineColor = when (layerName) {
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
            }).lineColor(lineColor)
    }

    private fun renderFeatureCollection(
        layerName: String, featureCollection: FeatureCollection, mapStyle: Style
    ) {
        if (featureCollection.features() != null) {
            // add the data itself to mapStyle
            mapStyle.addSource(
                GeoJsonSource.Builder(layerName).featureCollection(featureCollection).build()
            )

            if (mapTypeFromPreferences() == "satellite_view") {
                mapStyle.addLayer(createLineLayer(layerName))
            } else {
                // create a line layer that reads the GeoJSON data that we just added
                mapStyle.addLayerBelow(createLineLayer(layerName), "road-label")
            }

        }
    }

//    private fun cameraModeFromPreferences(): Int {
//        // extract string from strings.xml file (as integer key) and convert to string
//        val orientataionPreferenceKey = getResources().getString(R.string.map_orientation_preference_key)
//
//        // use key to extract saved camera mode preference string. Default to tracking compass,
//        // a.k.a. "Direction of Travel"
//        val orientationPreferenceString = PreferenceManager
//            .getDefaultSharedPreferences(this)
//            .getString(orientataionPreferenceKey, "direction_of_travel")
//
//       // convert into a MapBox camera mode and return
//        return if (orientationPreferenceString == "fixed") {
//            CameraMode.TRACKING
//        } else {
//            CameraMode.TRACKING_COMPASS
//        }
//    }

    private fun drawLocationOnMap() {

//        binding.mapView.location.apply {
//            locationPuck = createDefault2DPuck(activity, withBearing = true)

        val locationComponentPlugin = mapView?.location
        locationComponentPlugin?.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                topImage = AppCompatResources.getDrawable(
                    activity, com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
                ), bearingImage = AppCompatResources.getDrawable(
                    activity,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon,
                ), shadowImage = AppCompatResources.getDrawable(
                    activity,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon,
                ), scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
    }

    private fun mapTypeFromPreferences(): String? {
        // extract preference key string from strings.xml
        val mapTypePreferenceKey = getResources().getString(R.string.map_type_preference_key)

        // use that key to extract the stored user preference
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getString(mapTypePreferenceKey, "street_map")
    }

    private fun addRoutesAndLocation(style: Style) {
        // add geojson layers
        showMapLayers(this, style)

        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        checkLocationPermission {
            enableFollowRiderButton()
            drawLocationOnMap()
        }

    }

    private fun setupMapboxMap() {

        // apply map style conditionally, based on user's preferences.
        if (mapTypeFromPreferences() == "satellite_view") {
            mapView?.getMapboxMap()?.loadStyleUri(Style.SATELLITE) {
                    (addRoutesAndLocation(it))
                }
        } else {
            // pull custom street map styling from json source
            mapView?.getMapboxMap()?.loadStyleUri("asset://stylejson/style.json") {
                    (addRoutesAndLocation(it))
                }
        }
    }

    override fun onResume() {
        super.onResume()

//        Log.w("Apples", "On Resume Called")
//        // if the user is returning from the settings page, those settings will need to be applied
//        // TODO: solve onResume getting called in a loop
//        setupMapboxMap(updateStyleOnly = true)
//
//        // reload the autosleep preference in case it was changed while the activity was paused
//        setScreenModeFromPreferences()
    }
}

