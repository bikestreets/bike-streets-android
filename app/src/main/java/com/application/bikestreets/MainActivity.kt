package com.application.bikestreets

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.application.bikestreets.constants.PreferenceConstants
import com.application.bikestreets.composables.BottomSheetAndMap
import com.application.bikestreets.composables.SettingsModal
import com.application.bikestreets.theme.BikeStreetsTheme
import com.application.bikestreets.utils.PERMISSIONS_REQUEST_LOCATION
import com.application.bikestreets.utils.getDefaultPackageName
import com.application.bikestreets.utils.showToast
import com.mapbox.android.core.permissions.PermissionsManager

class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the UI content of this activity
        setContent {
            BikeStreetsTheme {
                MainUi()
            }
        }

        checkForLocationPermission()
    }

    private fun checkForLocationPermission() {
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    @Composable
    fun MainUi() {

        var showSettings by remember { mutableStateOf(false) }

        // TermsOfUse()
        if (showSettings) {
            SettingsModal(onCloseSettingsClicked = { showSettings = false })
        }
        BottomSheetAndMap(
            onSettingsClicked = { showSettings = true }
        )
    }

    private fun setScreenModeFromPreferences() {
        sharedPreferences = getSharedPreferences(getDefaultPackageName(this), MODE_PRIVATE)

        val keepScreenOnPreference =
            sharedPreferences.getBoolean(PreferenceConstants.KEEP_SCREEN_ON_PREFERENCE_KEY, true)

        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (keepScreenOnPreference) {
            window.addFlags(keepScreenOnFlag)
        } else {
            window.clearFlags(keepScreenOnFlag)
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferenceConstants.KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                setScreenModeFromPreferences()
            }

            PreferenceConstants.MAP_TYPE_PREFERENCE_KEY -> {
                // call this function, only to update the map style
                // TODO, check if this is responsive
//                loadMapboxStyle(mapView.getMapboxMap(), context = this)
            }

            else -> {
                Log.e(javaClass.simpleName, "No preference action for key: $key")
            }
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // TODO: Do we need to do anything with this knowledge?
            } else {
                showToast(this, getString(R.string.no_location_access))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }


    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("super.onBackPressed()", "androidx.activity.ComponentActivity")
    )
    override fun onBackPressed() {
        //TODO:
//        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
//            // Clear search and collapse
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            vm.clearText()
//        } else if (mapMarkersManager.hasMarkers) {
//            hideCurrentRouteLayer(mapView.getMapboxMap())
//            mapMarkersManager.clearMarkers()
//        } else {
        super.onBackPressed()
//        }
    }


}