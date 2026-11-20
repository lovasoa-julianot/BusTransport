package com.orion.bustransport.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("users/me")
    suspend fun getProfile(
    ): Response<UserResponse>

    @GET("tickets/driver")
    suspend fun getTodayTickets(
    ): Response<TicketResponse>

    @POST("tickets/scan")
    suspend fun validateTicket(
        @Body request: ScanRequest
    ): Response<ScanResponse>
}
