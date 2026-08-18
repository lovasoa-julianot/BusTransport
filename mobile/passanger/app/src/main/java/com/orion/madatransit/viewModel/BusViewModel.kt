package com.orion.madatransit.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.madatransit.model.BusStop
import com.orion.madatransit.repository.BusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class BusViewModel(private val repository: BusRepository) : ViewModel() {

    private val _stops = MutableStateFlow<List<BusStop>>(emptyList())
    val stops: StateFlow<List<BusStop>> = _stops.asStateFlow()

    private val _linePaths = MutableStateFlow<Map<String, List<GeoPoint>>>(emptyMap())
    val linePaths: StateFlow<Map<String, List<GeoPoint>>> = _linePaths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedStops = repository.getStops()
            _stops.value = fetchedStops
            
            val fetchedPaths = repository.getLinePaths()
            _linePaths.value = fetchedPaths
            _isLoading.value = false
        }
    }
}
