package com.example.bikestreets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.mapView
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import android.Manifest;
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import 	androidx.core.app.ActivityCompat
import android.widget.Toast
import com.esri.arcgisruntime.layers.KmlLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.ogc.kml.KmlDataset

class MainActivity : AppCompatActivity() {
    var mLocationDisplay: LocationDisplay ?= null
    var mArcGISMap: ArcGISMap ?= null

    val REPO_URL = "https://raw.githubusercontent.com/bikestreets/denver-map/master"
    val LAYERS_TO_LOAD = listOf(
        "1-bikestreets-master-v0.3.kml",
        "2-trails-master-v0.3.kml",
        "3-bikelanes-master-v0.3.kml",
        "4-bikesidewalks-master-v0.3.kml",
        "5-walk-master-v0.3.kml"
    )

    fun dataSourceStatusChangedHandler(dataSourceStatusChangedEvent: LocationDisplay.DataSourceStatusChangedEvent) {
        if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) return

        val requestPermissionsCode: Int = 2;
        val requestPermissions: Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        );

//        if (!(ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
        if(ContextCompat.checkSelfPermission(this, requestPermissions[0]) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, requestPermissions[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, requestPermissions, requestPermissionsCode);
        } else {
            var message: String = String.format(
                "Error in DataSourceStatusChangedListener: %s",
                dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().message
            );

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        mLocationDisplay?.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay?.startAsync();
    }

    fun setupLocationDisplay() {
        mLocationDisplay = mapView?.getLocationDisplay();

        mLocationDisplay?.addDataSourceStatusChangedListener(::dataSourceStatusChangedHandler);

        // activate auto-pan mode
        mLocationDisplay?.autoPanMode = LocationDisplay.AutoPanMode.RECENTER

        // start locationDisplay services
        mLocationDisplay?.startAsync()
    }

    fun addKmlLayers() {
        for(layerName in LAYERS_TO_LOAD) {
            // loop through all URIs to load
            var dataset = KmlDataset("$REPO_URL/$layerName")

            // add listener to report load errors
            dataset.addDoneLoadingListener(fun () {
                if (dataset.loadStatus != LoadStatus.LOADED) {
                    var error = "Failed to load kml layer from URL: " + dataset.loadError.message
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    Log.e("addKmlLayers", error)
                } else {
                    // press into a KML Layer
                    var kmlLayer = KmlLayer(dataset)

                    // add layer to the map's set of operational layers
                    mArcGISMap?.operationalLayers?.add(kmlLayer)
                }
            })

            // load from data source
            dataset.loadAsync()
        }
    }

    fun setupArcGISMap() {
        mArcGISMap = ArcGISMap(Basemap.Type.TOPOGRAPHIC, 87.740054, -104.946276, 16)

        addKmlLayers()

        mapView.map = mArcGISMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // create a map with the BasemapType topographic
        setupArcGISMap()

        // setup Location Display
        setupLocationDisplay()
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }
    override fun onResume() {
        super.onResume()
        mapView.resume()
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mLocationDisplay?.startAsync();
        } else {
            Toast.makeText(this, resources.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT)
        }
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
