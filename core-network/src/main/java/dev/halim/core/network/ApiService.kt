package dev.halim.core.network

import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.LibraryItemsResponse
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Result<LoginResponse>

    @GET("api/libraries")
    suspend fun libraries(): Result<LibrariesResponse>

    @GET("/api/libraries/{libraryId}/items")
    suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>

    @GET("/api/me")
    suspend fun me(): Result<User>

    fun generateItemCoverUrl(baseUrl: String, token: String, itemId: String): String {
        return "$baseUrl/api/items/$itemId/cover?token=$token"
    }

    fun generateItemStreamUrl(baseUrl: String, token: String, itemId: String, ino: String): String {
        return "$baseUrl/api/items/$itemId/file/$ino?token=$token"
    }
}