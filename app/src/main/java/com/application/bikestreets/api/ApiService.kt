package com.application.bikestreets.api

import com.application.bikestreets.api.modals.DirectionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/route/v1/driving/{startLongitude},{startLatitude};{endLongitude},{endLatitude}")
    suspend fun getRoute(
        @Path(value = "startLongitude", encoded = true) startLongitude: Double,
        @Path(value = "startLatitude", encoded = true) startLatitude: Double,
        @Path(value = "endLongitude", encoded = true) endLongitude: Double,
        @Path(value = "endLatitude", encoded = true) endLatitude: Double,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("alternatives") alternatives: String = "true",
        @Query("steps") steps: String = "true",
        @Query("annotations") annotations: String = "true",
    ): Response<DirectionResponse>
}