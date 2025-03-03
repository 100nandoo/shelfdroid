package dev.halim.core.network

import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Result<LoginResponse>
}