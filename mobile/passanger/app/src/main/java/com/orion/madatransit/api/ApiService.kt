package com.orion.madatransit.api

import com.google.gson.annotations.SerializedName
import com.orion.madatransit.model.NominatimResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
        private const val BASE_URL = "http://10.107.151.138:5000/api/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

interface NominatimApi {

    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("countrycodes") countryCodes: String = "mg"
    ): List<NominatimResult>

    companion object {
        private const val BASE_URL =
            "https://nominatim.openstreetmap.org/"

        private val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MadaTransit/1.0 (contact@orion.mg)")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val api: NominatimApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(NominatimApi::class.java)
        }
    }
}
