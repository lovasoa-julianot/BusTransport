package com.orion.madatransit.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.madatransit.api.NominatimApi
import com.orion.madatransit.model.BusStop
import com.orion.madatransit.model.NominatimResult
import com.orion.madatransit.repository.BusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class BusViewModel(
    private val repository: BusRepository
) : ViewModel() {

    private val _stops = MutableStateFlow<List<BusStop>>(emptyList())
    val stops: StateFlow<List<BusStop>> = _stops.asStateFlow()

    private val _linePaths =
        MutableStateFlow<Map<String, List<GeoPoint>>>(emptyMap())
    val linePaths: StateFlow<Map<String, List<GeoPoint>>> =
        _linePaths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allLines = MutableStateFlow<List<com.orion.madatransit.api.BusLineDto>>(emptyList())
    val allLines: StateFlow<List<com.orion.madatransit.api.BusLineDto>> = _allLines.asStateFlow()


    // =========================
    // Recherche Nominatim
    // =========================

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _suggestions =
        MutableStateFlow<List<NominatimResult>>(emptyList())

    val suggestions: StateFlow<List<NominatimResult>> =
        _suggestions.asStateFlow()

    private val _itineraries = MutableStateFlow<List<com.orion.madatransit.model.Itinerary>>(emptyList())
    val itineraries: StateFlow<List<com.orion.madatransit.model.Itinerary>> = _itineraries.asStateFlow()


    init {
        fetchData()
        observeSearch()
    }


    // =========================
    // Bus data
    // =========================

    fun fetchData() {
        viewModelScope.launch {

            _isLoading.value = true

            val fetchedStops = repository.getStops()
            _stops.value = fetchedStops

            val fetchedPaths = repository.getLinePaths()
            _linePaths.value = fetchedPaths

            val fetchedLines = repository.getLines()
            _allLines.value = fetchedLines

            _isLoading.value = false
        }
    }


    // =========================
    // Recherche
    // =========================

    fun onSearchChange(query: String) {
        _searchQuery.value = query

        // Si l'utilisateur efface le texte
        if (query.isBlank()) {
            _suggestions.value = emptyList()
        }
    }

    fun calculateItineraries(userPos: GeoPoint, targetStop: BusStop) {
        val allStops = _stops.value
        if (allStops.isEmpty()) return

        // 1. Trouver l'arrêt le plus proche de l'utilisateur
        val nearestStopToUser = allStops.minByOrNull { it.location.distanceToAsDouble(userPos) } ?: return
        
        val possibleItineraries = mutableListOf<com.orion.madatransit.model.Itinerary>()
        
        // 2. Parcourir toutes les lignes pour voir si elles relient nearestStopToUser -> targetStop
        _allLines.value.forEach { lineDto ->
            val stopsInLine = lineDto.lineStops ?: emptyList()
            val startIndex = stopsInLine.indexOfFirst { it.stop.id.toString() == nearestStopToUser.id }
            val endIndex = stopsInLine.indexOfFirst { it.stop.id.toString() == targetStop.id }

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                val stopCount = endIndex - startIndex
                val category = when {
                    stopCount <= 3 -> "Rapid"
                    stopCount <= 6 -> "Average"
                    else -> "High"
                }
                
                possibleItineraries.add(
                    com.orion.madatransit.model.Itinerary(
                        lineName = lineDto.name,
                        fromStopName = nearestStopToUser.name,
                        toStopName = targetStop.name,
                        category = category,
                        durationEstimated = "${stopCount * 5} min", // Simple heuristic
                        stopCount = stopCount,
                        path = lineDto.roadPath.map { GeoPoint(it[0], it[1]) }
                    )
                )
            }
        }
        
        _itineraries.value = possibleItineraries.sortedBy { it.stopCount }
    }


    private fun observeSearch() {

        viewModelScope.launch {

            searchQuery
                .debounce(200)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collectLatest { query ->

                    try {

                        val results =
                            NominatimApi.api.searchPlaces(query)

                        _suggestions.value = results

                    } catch (e: Exception) {

                        Log.e(
                            "NOMINATIM",
                            "Erreur recherche",
                            e
                        )

                        _suggestions.value =
                            emptyList()
                    }
                }
        }
    }
}