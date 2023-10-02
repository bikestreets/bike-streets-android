package com.application.bikestreets.utils

import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.search.SearchOptions
import com.mapbox.search.common.IsoCountryCode

fun getSearchOptions(): SearchOptions {
    // Setting up Bounding box
    val southwest = Point.fromLngLat(-105.214691, 39.516437)
    val northEast = Point.fromLngLat(-104.582977, 39.915740)

    val boundingBox = BoundingBox.fromPoints(southwest, northEast)

    return SearchOptions(
        countries = listOf(IsoCountryCode.UNITED_STATES), boundingBox = boundingBox
    )
}