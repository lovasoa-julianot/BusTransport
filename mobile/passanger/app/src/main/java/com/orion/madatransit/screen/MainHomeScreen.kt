package com.orion.madatransit.screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orion.madatransit.api.ApiService
import com.orion.madatransit.repository.BusRepository
import com.orion.madatransit.viewModel.BusViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.orion.madatransit.components.BusStopRowItem
import com.orion.madatransit.components.SelectedStopBanner
import com.orion.madatransit.model.BusStop
import com.orion.madatransit.model.NominatimResult
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MainHomeScreen(
    onNavigateToItinerary: (destination: String) -> Unit,
    viewModel: BusViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val apiService = ApiService.create()
                val repository = BusRepository(apiService)
                return BusViewModel(repository) as T
            }
        }
    )
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val allLinePaths by viewModel.linePaths.collectAsStateWithLifecycle()
    val itineraries by viewModel.itineraries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Permisssions handling
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Configuration d'OSM
    Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", Context.MODE_PRIVATE))

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    var selectedStop by remember { mutableStateOf<BusStop?>(null) }
    var selectedItinerary by remember { mutableStateOf<com.orion.madatransit.model.Itinerary?>(null) }
    
    // Default position if permission is denied
    val defaultPosition = GeoPoint(-21.4536, 47.0858)
    var userPosition by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Use fetched road path based on selected line or itinerary if available
    val roadPath = remember(selectedStop, selectedItinerary, allLinePaths) {
        val itinerary = selectedItinerary
        if (itinerary != null) {
            itinerary.path
        } else {
            val currentStop = selectedStop
            if (currentStop != null) {
                // Find path for one of the lines of the selected stop
                val line = currentStop.availableLines.split(",").firstOrNull()?.trim()
                allLinePaths[line] ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userPosition = GeoPoint(location.latitude, location.longitude)
                        mapView?.controller?.animateTo(userPosition)
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionsState.launchMultiplePermissionRequest()
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    val stopsWithDistance = remember(stops, userPosition) {
        val pos = userPosition ?: defaultPosition
        stops.map { stop ->
            val dist = stop.location.distanceToAsDouble(pos)
            val distStr = if (dist < 1000) "${dist.toInt()} m" else "%.1f km".format(dist / 1000.0)
            stop.copy(distance = distStr)
        }.sortedBy { it.location.distanceToAsDouble(pos) }
    }

    val filteredStops = remember(searchQuery, stopsWithDistance) {
        if (searchQuery.isBlank()) stopsWithDistance
        else stopsWithDistance.filter { it.name.contains(searchQuery, ignoreCase = true) || it.availableLines.contains(searchQuery) }
    }

    val effectiveUserPosition = userPosition ?: defaultPosition
    val recommendedStop = remember(stopsWithDistance) {
        stopsWithDistance.firstOrNull()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = Color.White,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 40.dp,
                height = 4.dp,
                color = Color.LightGray
            )
        },
        sheetContent = {
            BottomSheetContent(
                selectedStop = selectedStop,
                filteredStops = filteredStops,
                recommendedStop = recommendedStop,
                itineraries = itineraries,
                selectedItinerary = selectedItinerary,
                primaryColor = primaryColor,
                onStopSelected = { stop ->
                    selectedStop = stop
                    selectedItinerary = null
                    viewModel.calculateItineraries(userPosition ?: defaultPosition, stop)
                    mapView?.controller?.animateTo(stop.location)
                },
                onItinerarySelected = { itinerary ->
                    selectedItinerary = itinerary
                },
                onCloseBanner = { 
                    selectedStop = null
                    selectedItinerary = null
                },
                onNavigate = onNavigateToItinerary
            )
        }
    ) { innerPadding ->
        MainContent(
            innerPadding = innerPadding,
            primaryColor = primaryColor,
            searchQuery = searchQuery,
            suggestions = suggestions,
            onSearchChange = { query ->
                viewModel.onSearchChange(query)
                if (query.isNotEmpty()) {
                    selectedStop = null
                }
            },
            onSuggestionClick = { result ->
                val lat = result.lat.toDoubleOrNull()
                val lon = result.lon.toDoubleOrNull()
                if (lat != null && lon != null) {
                    val point = GeoPoint(lat, lon)
                    mapView?.controller?.animateTo(point)
                    mapView?.controller?.setZoom(16.0)
                    
                    // Clear search and suggestions after selection
                    viewModel.onSearchChange("") 
                }
            },

            userPosition = effectiveUserPosition,
            isLocationKnown = userPosition != null,
            filteredStops = filteredStops,
            roadPath = roadPath,
            selectedStop = selectedStop,
            selectedItinerary = selectedItinerary,
            onStopSelected = { selectedStop = it },
            mapView = mapView,
            onMapViewReady = { mapView = it },
            isLoading = isLoading
        )
    }
}

@Composable
fun BottomSheetContent(
    selectedStop: BusStop?,
    filteredStops: List<BusStop>,
    recommendedStop: BusStop?,
    itineraries: List<com.orion.madatransit.model.Itinerary>,
    selectedItinerary: com.orion.madatransit.model.Itinerary?,
    primaryColor: Color,
    onStopSelected: (BusStop) -> Unit,
    onItinerarySelected: (com.orion.madatransit.model.Itinerary) -> Unit,
    onCloseBanner: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        if (selectedStop != null) {
            SelectedStopBanner(
                stop = selectedStop,
                onClose = onCloseBanner,
                onGoToItinerary = { onNavigate(selectedStop.name) }
            )
            
            if (itineraries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Itinéraires suggérés",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(itineraries) { itinerary ->
                        val isSelected = selectedItinerary == itinerary
                        ItineraryRowItem(
                            itinerary = itinerary,
                            isSelected = isSelected,
                            primaryColor = primaryColor,
                            onClick = { onItinerarySelected(itinerary) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aucun itinéraire direct trouvé", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            if (recommendedStop != null && filteredStops.contains(recommendedStop)) {
                RecommendationCard(recommendedStop, primaryColor) { onStopSelected(recommendedStop) }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Arrêts proches", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${filteredStops.size} arrêts trouvés autour de vous", fontSize = 12.sp, color = Color.Gray)
                }
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(filteredStops) { stop ->
                    BusStopRowItem(
                        stop = stop,
                        onClick = { onStopSelected(stop) }
                    )
                }
            }
        }
    }
}

@Composable
fun ItineraryRowItem(
    itinerary: com.orion.madatransit.model.Itinerary,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.White
        ),
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 0.dp) else CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, primaryColor) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = primaryColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = itinerary.lineName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(itinerary.category) {
                            "Rapid" -> "Rapide"
                            "Average" -> "Moyenne"
                            else -> "Long"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when(itinerary.category) {
                            "Rapid" -> Color(0xFF2E7D32)
                            "Average" -> Color(0xFFEF6C00)
                            else -> Color(0xFFC62828)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "De ${itinerary.fromStopName}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "${itinerary.stopCount} arrêts • ${itinerary.durationEstimated}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = if (isSelected) primaryColor else Color.LightGray
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(stop: BusStop, primaryColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Meilleur choix proche", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                Text(stop.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${stop.distance} • ${stop.availableLines}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MainContent(
    innerPadding: PaddingValues,
    primaryColor: Color,
    searchQuery: String,
    suggestions: List<NominatimResult>,
    onSuggestionClick: (NominatimResult) -> Unit,
    onSearchChange: (String) -> Unit,
    userPosition: GeoPoint,
    isLocationKnown: Boolean,
    filteredStops: List<BusStop>,
    roadPath: List<GeoPoint>,
    selectedStop: BusStop?,
    selectedItinerary: com.orion.madatransit.model.Itinerary?,
    onStopSelected: (BusStop) -> Unit,
    mapView: MapView?,
    onMapViewReady: (MapView) -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .zIndex(2f),
                color = primaryColor
            )
        }
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    controller.setCenter(userPosition)

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                    
                    onMapViewReady(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.overlays.removeIf { it is Marker || it is Polyline }
                
                // Tracé du chemin du bus (Polyline) if a stop is selected
                if (selectedStop != null) {
                    val polyline = Polyline(view)
                    polyline.setPoints(roadPath)
                    val colorInt = primaryColor.value.toLong().toInt()
                    polyline.outlinePaint.color = colorInt
                    polyline.outlinePaint.strokeWidth = 10f
                    polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
                    view.overlays.add(polyline)
                }

                // Add User Position Marker if known
                if (isLocationKnown) {
                    val userMarker = Marker(view)
                    userMarker.position = userPosition
                    userMarker.title = "Ma Position"
                    val icon = view.context.getDrawable(android.R.drawable.ic_menu_myplaces)?.apply {
                        setTint(android.graphics.Color.BLUE)
                    }
                    userMarker.icon = icon
                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    view.overlays.add(userMarker)
                }

                // Add Bus Stop Markers
                filteredStops.forEach { stop ->
                    val marker = Marker(view)
                    marker.position = stop.location
                    marker.title = stop.name
                    
                    val isStartOfItinerary = selectedItinerary?.fromStopName == stop.name
                    if (isStartOfItinerary) {
                        marker.icon = view.context.getDrawable(android.R.drawable.ic_dialog_map)?.apply {
                            setTint(android.graphics.Color.GREEN)
                        }
                    }

                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { m, _ ->
                        onStopSelected(stop)
                        view.controller.animateTo(m.position)
                        true
                    }
                    view.overlays.add(marker)
                }
                view.invalidate()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            HeaderSection(primaryColor, searchQuery, suggestions, onSearchChange, onSuggestionClick, isLocationKnown)
        }

        FloatingActionButton(
            onClick = {
                mapView?.controller?.animateTo(userPosition)
                mapView?.controller?.setZoom(15.0)
            },
            containerColor = Color.White,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(48.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = primaryColor)
        }
    }
}

@Composable
fun HeaderSection(
    primaryColor: Color,
    searchQuery: String,
    suggestions: List<NominatimResult>,
    onSearchChange: (String) -> Unit,
    onSuggestionClick: (NominatimResult) -> Unit,
    isLocationKnown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isLocationKnown) {
                        primaryColor
                    } else {
                        Color.Gray
                    }
                )
                Spacer( modifier = Modifier.width(8.dp) )
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            "Où allez-vous ?",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.DarkGray,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = Color.LightGray)

                Column(modifier = Modifier.heightIn(max = 300.dp)) {
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion.displayName ?: "Lieu inconnu",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}