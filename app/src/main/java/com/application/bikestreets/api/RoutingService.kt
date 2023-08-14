package com.application.bikestreets.api

import android.util.Log
import com.application.bikestreets.api.modals.DirectionResponse
import com.mapbox.geojson.Point
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RoutingService {

    //TODO: this shouldn't need to be inside a companion object
    companion object {
        suspend fun getRoutingDirections(
            startCoordinates: Point,
            endCoordinates: Point
        ): DirectionResponse? {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://206.189.205.9")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            try {
                // API and Points use format List[Longitude, Latitude]
                val response = apiService.getRoute(
                    startCoordinates.coordinates()[0],
                    startCoordinates.coordinates()[1],
                    endCoordinates.coordinates()[0],
                    endCoordinates.coordinates()[1]
                )

                if (response.isSuccessful) {
                    Log.i(
                        javaClass.simpleName,
                        "getRoutingDirections request url(${response.code()}): ${response.raw().request.url}"
                    )
                    return response.body()
                    // Handle the response
                } else {
                    Log.e(
                        javaClass.simpleName,
                        "getRoutingDirections response error (${response.code()}): ${response.message()} , ${response.raw().request.url}"
                    )
                    // Handle non-successful response
                }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "getRoutingDirections call error: $e")
                // Handle other errors
            }
            // If any error, null is returned
            return null
        }
    }
}