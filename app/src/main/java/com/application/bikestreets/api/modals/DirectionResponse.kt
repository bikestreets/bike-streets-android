package com.application.bikestreets.api.modals

data class DirectionResponse(
    val code: String,
    val waypoints: List<WayPoint>,
    val routes: List<Route>,
)

data class WayPoint(
    val hit: String,
    val distance: Double,
    val name: String,
    val location: List<Double>
)

data class Route(
    val legs: List<Legs>,
    val weight_name: String,
    val weight: Number,
    val duration: Number,
    val distance: Number,
)

data class Legs(
    val steps: List<Steps>,
    val summary: String,
    val weight: Number,
    val duration: Number,
//            val annotation:
    val distance: Number,
)

data class Steps(
    val geometry: Geometry,
//    val manuver: Manuver
    val mode: String,
    val driving_side: String,
    val name: String,
//    val intersections
)

data class Geometry(
    val coordinates: List<List<Double>>,
    val type: String,
)
