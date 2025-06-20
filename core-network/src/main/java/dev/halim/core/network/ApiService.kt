package dev.halim.core.network

import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.request.SyncSessionRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.LibraryItemsResponse
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.LogoutResponse
import dev.halim.core.network.response.User
import dev.halim.core.network.response.play.PlayResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
  @POST("login") suspend fun login(@Body request: LoginRequest): Result<LoginResponse>

  @POST("logout") suspend fun logout(): Result<LogoutResponse>

  //  libraries
  @GET("api/libraries") suspend fun libraries(): Result<LibrariesResponse>

  @GET("/api/libraries/{libraryId}/items")
  suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>

  //  items
  @GET("/api/items/{itemId}")
  suspend fun item(
    @Path("itemId") itemId: String,
    @Query("expanded") expanded: Int = 1,
  ): Result<LibraryItem>

  @POST("api/items/batch/get")
  suspend fun batchLibraryItems(
    @Body request: BatchLibraryItemsRequest
  ): Result<BatchLibraryItemsResponse>

  @POST("api/items/{itemId}/play")
  suspend fun playBook(@Path("itemId") itemId: String, @Body request: PlayRequest): PlayResponse

  @POST("api/items/{itemId}/play/{episodeId}")
  suspend fun playPodcast(
    @Path("itemId") itemId: String,
    @Path("episodeId") episodeId: String,
    @Body request: PlayRequest,
  ): PlayResponse

  //  me
  @GET("/api/me") suspend fun me(): Result<User>

  @PATCH("/api/me/progress/{itemId}")
  suspend fun patchBookProgress(
    @Path("itemId") itemId: String,
    @Body request: ProgressRequest,
  ): Result<Unit>

  @PATCH("/api/me/progress/{itemId}/{episodeId}")
  suspend fun patchPodcastProgress(
    @Path("itemId") itemId: String,
    @Path("episodeId") episodeId: String,
    @Body request: ProgressRequest,
  ): Result<Unit>

  // session

  @POST("/api/session/{sessionId}/sync")
  suspend fun syncSession(
    @Path("sessionId") sessionId: String,
    @Body request: SyncSessionRequest,
  ): Result<Unit>
}
