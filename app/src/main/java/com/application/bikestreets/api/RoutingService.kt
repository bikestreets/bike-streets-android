package com.application.bikestreets.api

import android.util.Log
import com.application.bikestreets.api.modals.DirectionResponse
import com.mapbox.geojson.Point
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RoutingService {

    suspend fun getRoutingDirections(
        startCoordinates: Point,
        endCoordinates: Point
    ): DirectionResponse? {

        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://206.189.205.9")
            .client(client)
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
                return response.body()

            }
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "getRoutingDirections call error: $e")
            // Handle other errors
        }
        // If any error, null is returned
        return null
    }
}