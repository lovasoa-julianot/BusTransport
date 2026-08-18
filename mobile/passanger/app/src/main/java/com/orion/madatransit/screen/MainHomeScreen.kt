package com.orion.madatransit.screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.background
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedStop by remember { mutableStateOf<BusStop?>(null) }
    
    // Default position if permission is denied
    val defaultPosition = GeoPoint(-21.4536, 47.0858)
    var userPosition by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Use fetched road path based on selected line if available
    val roadPath = remember(selectedStop, allLinePaths) {
        val currentStop = selectedStop
        if (currentStop != null) {
            // Find path for one of the lines of the selected stop
            val line = currentStop.availableLines.split(",").firstOrNull()?.trim()
            allLinePaths[line] ?: emptyList()
        } else {
            emptyList()
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
                primaryColor = primaryColor,
                onStopSelected = { stop ->
                    selectedStop = stop
                    mapView?.controller?.animateTo(stop.location)
                },
                onCloseBanner = { selectedStop = null },
                onNavigate = onNavigateToItinerary
            )
        }
    ) { innerPadding ->
        MainContent(
            innerPadding = innerPadding,
            primaryColor = primaryColor,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            userPosition = effectiveUserPosition,
            isLocationKnown = userPosition != null,
            filteredStops = filteredStops,
            roadPath = roadPath,
            selectedStop = selectedStop,
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
    primaryColor: Color,
    onStopSelected: (BusStop) -> Unit,
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
    onSearchChange: (String) -> Unit,
    userPosition: GeoPoint,
    isLocationKnown: Boolean,
    filteredStops: List<BusStop>,
    roadPath: List<GeoPoint>,
    selectedStop: BusStop?,
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
            HeaderSection(primaryColor, searchQuery, onSearchChange, isLocationKnown)
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
fun HeaderSection(primaryColor: Color, searchQuery: String, onSearchChange: (String) -> Unit, isLocationKnown: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = if (isLocationKnown) primaryColor else Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Où allez-vous ?", fontSize = 16.sp, color = Color.Gray) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Gray,
                    unfocusedTextColor = Color.Gray,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
