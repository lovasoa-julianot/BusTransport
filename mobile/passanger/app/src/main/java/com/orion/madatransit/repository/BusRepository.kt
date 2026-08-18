package com.orion.madatransit.repository

import androidx.compose.ui.graphics.Color
import com.orion.madatransit.api.ApiService
import com.orion.madatransit.model.BusStop
import org.osmdroid.util.GeoPoint

class BusRepository(private val apiService: ApiService) {

    suspend fun getStops(): List<BusStop> {
        return try {
            val stopsResponse = apiService.getStops()
            val linesResponse = apiService.getLines()

            val stops = stopsResponse.data
            val lines = linesResponse.data

            // Create a mapping of Stop ID to list of line names passing through it
            val stopLinesMap = mutableMapOf<Int, MutableList<String>>()
            lines.forEach { line ->
                line.lineStops?.forEach { lineStop ->
                    stopLinesMap.getOrPut(lineStop.stop.id) { mutableListOf() }.add(line.name)
                }
            }

            stops.map { dto ->
                BusStop(
                    id = dto.id.toString(),
                    name = dto.name,
                    location = GeoPoint(
                        dto.latitude.toDoubleOrNull() ?: 0.0,
                        dto.longitude.toDoubleOrNull() ?: 0.0
                    ),
                    distance = "Calcul...",
                    availableLines = stopLinesMap[dto.id]?.distinct()?.joinToString(", ") ?: "Aucune ligne",
                    distanceColor = Color(0xFF00C853)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getLinePaths(): Map<String, List<GeoPoint>> {
        return try {
            val response = apiService.getLines()
            response.data.associate { dto ->
                dto.name to dto.roadPath.map { point ->
                    GeoPoint(point[0], point[1])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}
