package com.orion.madatransit.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import org.osmdroid.util.GeoPoint

data class OnboardingPage(
    val title: String,
    val description: String? = null,
    val backgroundColor: Long,
    val isSplashScreen: Boolean = false
)

data class BusStop(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val distance: String,
    val availableLines: String,
    val distanceColor: Color,
//    val description : String
)

data class BusStopInfo(
    val name: String,
    val distance: String,
    val availableLines: String,
    val distanceColor: Color
)

data class NominatimResult(
    @SerializedName("place_id")
    val placeId: Long,

    @SerializedName("display_name")
    val displayName: String = "",

    val lat: String = "0.0",
    val lon: String = "0.0"
)

data class Itinerary(
    val lineName: String,
    val fromStopName: String,
    val toStopName: String,
    val category: String, // Rapid, Average, High
    val durationEstimated: String,
    val stopCount: Int,
    val path: List<GeoPoint>
)
