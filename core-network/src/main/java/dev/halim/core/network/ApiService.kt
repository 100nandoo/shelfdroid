package dev.halim.core.network

import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
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
    suspend fun login(@Body request: LoginRequest): Result<LoginResponse>

    @GET("api/libraries")
    suspend fun libraries(): Result<LibrariesResponse>

    @GET("/api/libraries/{libraryId}/items")
    suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>

    @POST("api/items/batch/get")
    suspend fun batchLibraryItems(@Body request: BatchLibraryItemsRequest): Result<BatchLibraryItemsResponse>

    @GET("/api/me")
    suspend fun me(): Result<User>
}