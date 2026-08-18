package com.orion.madatransit.api

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ApiResponse<T>(
    val success: Boolean,
    val count: Int,
    val data: T
)

data class BusStopDto(
    @SerializedName("id_stop") val id: Int,
    @SerializedName("name_stop") val name: String,
    val latitude: String,
    val longitude: String
)

data class BusLineDto(
    @SerializedName("id_line") val id: Int,
    @SerializedName("line_name") val name: String,
    @SerializedName("road_path") val roadPath: List<List<Double>>,
    val lineStops: List<LineStopDto>? = null
)

data class LineStopDto(
    @SerializedName("id_line_stop") val id: Int,
    val stop: BusStopDto
)

interface ApiService {
    @GET("stops")
    suspend fun getStops(): ApiResponse<List<BusStopDto>>

    @GET("lines")
    suspend fun getLines(): ApiResponse<List<BusLineDto>>

    companion object {
        private const val BASE_URL = "http://192.168.0.124:5000/api/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
