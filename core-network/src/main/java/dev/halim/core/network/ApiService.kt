package dev.halim.core.network

import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.LibraryItemsResponse
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.LogoutResponse
import dev.halim.core.network.response.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Result<LoginResponse>

    @POST("logout")
    suspend fun logout(): Result<LogoutResponse>

    @GET("api/libraries")
    suspend fun libraries(): Result<LibrariesResponse>

    @GET("/api/libraries/{libraryId}/items")
    suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>

    @GET("/api/items/{itemId}")
    suspend fun item(
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1
    ): Result<LibraryItem>

    @POST("api/items/batch/get")
    suspend fun batchLibraryItems(@Body request: BatchLibraryItemsRequest): Result<BatchLibraryItemsResponse>

    @GET("/api/me")
    suspend fun me(): Result<User>
}