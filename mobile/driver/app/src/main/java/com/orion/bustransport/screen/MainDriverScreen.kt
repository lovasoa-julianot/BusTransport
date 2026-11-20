package com.orion.bustransport.screen

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orion.bustransport.network.RetrofitClient
import com.orion.bustransport.utils.TextToSpeechManager
import com.orion.bustransport.viewModel.DriverViewModel
import com.orion.bustransport.R
import com.orion.bustransport.network.User
import com.orion.bustransport.components.ErrorSplashScreen

sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Scan : Screen("scan", "Scanner", R.drawable.code)
    object Revenue : Screen("revenue", "Revenus", R.drawable.income)
    object Profile : Screen("profile", "Profil", R.drawable.user)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDriverScreen(driverName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scan) }

    val viewModel: DriverViewModel = viewModel(factory = DriverViewModel.Factory)
    val uiState = viewModel.uiState

    if (uiState.isFatalError) {
        ErrorSplashScreen(
            errorMessage = uiState.error ?: "Impossible de contacter le serveur. L'application va se fermer.",
            onExit = { (context as? Activity)?.finish() }
        )
        return // Stop rendering the rest of the screen
    }

    val items = listOf(Screen.Scan, Screen.Revenue, Screen.Profile)
    val ttsManager = remember { TextToSpeechManager(context) }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentScreen == Screen.Scan) "Contrôle des Tickets" else if (currentScreen == Screen.Revenue) "Rapport Journalier" else "Informations personnel",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF009688),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xE8EAEAEA),
                contentColor = Color(0xFF009688)
            ) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Image(
                                painter = painterResource(id = screen.icon),
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(
                                    if (currentScreen == screen) {
                                        Color(0xFF00B29A)
                                    } else {
                                        Color.Gray
                                    }
                                )
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (currentScreen == screen) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF00B29A),
                            indicatorColor = Color(0xFFEAEAEA),
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                is Screen.Scan -> {
                    if (uiState.loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00B29A))
                        }
                    } else if (uiState.user != null) {
                        ScanScreen(
                            user = uiState.user,
                            driverName = uiState.user.username,
                            scanResult = uiState.scanResult,
                            scanResultId = uiState.scanResultId,
                            onTokenScanned = {
                                viewModel.scanTicket(it)
                            },
                            onClearScanResult = {
                                viewModel.clearScanResult()
                            },
                            imageBaseUrl = RetrofitClient.getBaseURL()
                        )
                    }
                }
                is Screen.Revenue -> {
                    uiState.user?.let {
                        RevenueScreen(
                            totalTicketsScanned = uiState.totalTickets,
                            ticketPriceMGA = 2000,
                            tickets = uiState.tickets
                        )
                    }
                }
                else -> {
                    uiState.user?.let {
                        ProfileScreen(
                            user = it,
                            onUpdateProfile = {},
                            onBack = { currentScreen = Screen.Scan },
                            onLogout = {
                                viewModel.logout {
                                    onLogout()
                                }
                            },
                            imageBaseUrl = RetrofitClient.getBaseURL()
                        )
                    }
                }
            }
        }
    }
}