package com.orion.bustransport.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.Gson
import com.orion.bustransport.network.DriverUiState
import com.orion.bustransport.network.RetrofitClient
import com.orion.bustransport.network.ScanRequest
import com.orion.bustransport.network.ScanResponse
import com.orion.bustransport.repository.DriverRepository
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DriverViewModel(
    private val repository: DriverRepository
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = DriverRepository(RetrofitClient.apiService)
                DriverViewModel(repository)
            }
        }
    }

    var uiState by mutableStateOf(DriverUiState())
        private set

    init {
        loadData()
    }

    private var scanCounter = 0L

    private fun nextScanResultId(): Long {
        scanCounter++
        return scanCounter
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null, isFatalError = false)
            try {
                kotlinx.coroutines.delay(500)

                val user = repository.getProfile()
                val tickets = repository.getTodayTickets()

                uiState = uiState.copy(
                    loading = false,
                    user = user,
                    tickets = tickets,
                    totalTickets = tickets.size
                )
            } catch (e: Exception) {
                println("DEBUG: loadData exception: ${e.message}")
                val isConnectionError = e is ConnectException || 
                                      e is UnknownHostException || 
                                      e is SocketTimeoutException
                
                uiState = uiState.copy(
                    loading = false,
                    error = "Erreur de connexion au serveur: ${e.message}",
                    isFatalError = isConnectionError
                )
            }
        }
    }

    fun clearScanResult() {
        uiState = uiState.copy(
            scanResult = null
        )
    }

    fun scanTicket(qrCode: String) {
        viewModelScope.launch {
            try {
                val response = repository.scanTicket(
                    ScanRequest(qrcode = qrCode)
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        loadData()
                        uiState = uiState.copy(
                            scanResult = Pair(true, body.message),
                            scanResultId = nextScanResultId()
                        )
                    } else {
                        uiState = uiState.copy(
                            scanResult = Pair(false, body?.error ?: "Unknown error"),
                            scanResultId = nextScanResultId()
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = try {
                        Gson().fromJson(errorBody, ScanResponse::class.java)
                    } catch (_: Exception) {
                        null
                    }
                    
                    val displayMessage = errorResponse?.error ?: errorResponse?.message ?: "Server error: ${response.code()}"
                    uiState = uiState.copy(
                        scanResult = Pair(false, displayMessage),
                        scanResultId = nextScanResultId()
                    )
                }
            } catch (e: Exception) {
                val isConnectionError = e is ConnectException || 
                                      e is UnknownHostException || 
                                      e is SocketTimeoutException
                
                uiState = uiState.copy(
                    scanResult = Pair(false, "Network error: ${e.message}"),
                    scanResultId = nextScanResultId(),
                    error = if (isConnectionError) "Erreur de connexion au serveur" else null,
                    isFatalError = isConnectionError
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            RetrofitClient.tokenManager.clearToken()
            onComplete()
        }
    }
}