package dev.halim.core.network

import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.request.BookmarkRequest
import dev.halim.core.network.request.ChangePasswordRequest
import dev.halim.core.network.request.CoverFromUrlRequest
import dev.halim.core.network.request.CreateApiKeyRequest
import dev.halim.core.network.request.CreatePodcastRequest
import dev.halim.core.network.request.CreateUserRequest
import dev.halim.core.network.request.DeleteSessionsRequest
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.request.MatchLibraryItemRequest
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.request.SyncLocalAllSessionRequest
import dev.halim.core.network.request.SyncLocalSessionRequest
import dev.halim.core.network.request.SyncSessionRequest
import dev.halim.core.network.request.UpdateApiKeyRequest
import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.request.UpdateServerSettingsRequest
import dev.halim.core.network.request.UpdateUserRequest
import dev.halim.core.network.response.ApiKeysResponse
import dev.halim.core.network.response.AudioBookmark
import dev.halim.core.network.response.BackupsResponse
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.CreateUpdateApiKeyResponse
import dev.halim.core.network.response.CreateUserResponse
import dev.halim.core.network.response.DeleteUserResponse
import dev.halim.core.network.response.Episode
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.LibraryItemsResponse
import dev.halim.core.network.response.LibrarySeriesResponse
import dev.halim.core.network.response.ListeningStatResponse
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.LogoutResponse
import dev.halim.core.network.response.LogsResponse
import dev.halim.core.network.response.MatchItemResult
import dev.halim.core.network.response.OpenSessionsResponse
import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.core.network.response.SearchCoversResponse
import dev.halim.core.network.response.SearchPodcast
import dev.halim.core.network.response.SearchProvidersResponse
import dev.halim.core.network.response.ServerSettingsResponse
import dev.halim.core.network.response.SessionsResponse
import dev.halim.core.network.response.SetItemCoverResponse
import dev.halim.core.network.response.SyncLocalAllSessionResponse
import dev.halim.core.network.response.TagsResponse
import dev.halim.core.network.response.UpdateUserResponse
import dev.halim.core.network.response.User
import dev.halim.core.network.response.UserWithMediaProgressDetail
import dev.halim.core.network.response.UsersResponse
import dev.halim.core.network.response.play.PlayResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

  // api keys
  @GET("api/api-keys") suspend fun apiKeys(): Result<ApiKeysResponse>

  @POST("api/api-keys")
  suspend fun createApiKey(@Body request: CreateApiKeyRequest): Result<CreateUpdateApiKeyResponse>

  @PATCH("api/api-keys/{apiKeyId}")
  suspend fun updateApiKey(
    @Path("apiKeyId") apiKeyId: String,
    @Body request: UpdateApiKeyRequest,
  ): Result<CreateUpdateApiKeyResponse>

  @DELETE("api/api-keys/{apiKeyId}")
  suspend fun deleteApiKey(@Path("apiKeyId") apiKeyId: String): Result<Unit>

  //  auth
  @POST("login")
  suspend fun login(
    @Body request: LoginRequest,
    @Header("x-return-tokens") returnTokens: String = "true",
  ): Result<LoginResponse>

  @POST("api/authorize")
  suspend fun authorize(
    @Header("x-return-tokens") returnTokens: String = "true"
  ): Result<LoginResponse>

  @POST("auth/refresh")
  suspend fun refresh(@Header("x-refresh-token") refreshToken: String): Result<LoginResponse>

  @POST("logout")
  suspend fun logout(@Header("x-refresh-token") refreshToken: String): Result<LogoutResponse>

  // backups
  @GET("/api/backups") suspend fun backups(): Result<BackupsResponse>

  @POST("/api/backups") suspend fun createBackup(): Result<BackupsResponse>

  @DELETE("/api/backups/{backupId}")
  suspend fun deleteBackup(@Path("backupId") backupId: String): Result<BackupsResponse>

  @GET("/api/backups/{backupId}/apply")
  suspend fun applyBackup(@Path("backupId") backupId: String): Result<Unit>

  @Multipart
  @POST("/api/backups/upload")
  suspend fun uploadBackup(@Part file: MultipartBody.Part): Result<Unit>

  //  libraries
  @GET("api/libraries") suspend fun libraries(): Result<LibrariesResponse>

  @GET("/api/libraries/{libraryId}/items")
  suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>

  @GET("/api/libraries/{libraryId}/series")
  suspend fun librarySeries(
    @Path("libraryId") libraryId: String,
    @Query("limit") limit: Int = 100,
    @Query("page") page: Int = 0,
    @Query("sort") sort: String = "name",
    @Query("desc") desc: Int = 0,
  ): Result<LibrarySeriesResponse>

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

  @DELETE("/api/items/{itemId}")
  suspend fun deleteItem(@Path("itemId") itemId: String, @Query("hard") hard: Int = 0): Result<Unit>

  @PATCH("/api/items/{itemId}/media")
  suspend fun updateItemMedia(
    @Path("itemId") itemId: String,
    @Body request: UpdateLibraryItemMediaRequest,
  ): Result<Unit>

  @POST("/api/items/{itemId}/match")
  suspend fun matchItem(
    @Path("itemId") itemId: String,
    @Body request: MatchLibraryItemRequest,
  ): Result<MatchItemResult>

  @POST("/api/items/{itemId}/scan")
  suspend fun reScanItem(@Path("itemId") itemId: String): Result<Unit>

  @Multipart
  @POST("/api/items/{itemId}/cover")
  suspend fun uploadItemCover(
    @Path("itemId") itemId: String,
    @Part cover: MultipartBody.Part,
  ): Result<LibraryItem>

  @POST("/api/items/{itemId}/cover")
  suspend fun setItemCoverFromUrl(
    @Path("itemId") itemId: String,
    @Body request: CoverFromUrlRequest,
  ): Result<SetItemCoverResponse>

  @DELETE("/api/items/{itemId}/cover")
  suspend fun deleteItemCover(@Path("itemId") itemId: String): Result<Unit>

  @POST("/api/tools/item/{itemId}/embed-metadata")
  suspend fun embedItemMetadata(@Path("itemId") itemId: String): Result<Unit>

  @GET("/api/search/books")
  suspend fun searchBooks(
    @Query("provider") provider: String,
    @Query("title") title: String,
    @Query("author") author: String? = null,
  ): Result<List<SearchBookMatchResponse>>

  @GET("/api/search/covers")
  suspend fun searchCovers(
    @Query("title") title: String,
    @Query("author") author: String? = null,
    @Query("provider") provider: String? = null,
    @Query("podcast") podcast: Int = 0,
  ): Result<SearchCoversResponse>

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

  @POST("/api/me/item/{itemId}/bookmark")
  suspend fun createBookmark(
    @Path("itemId") itemId: String,
    @Body request: BookmarkRequest,
  ): Result<AudioBookmark>

  @PATCH("/api/me/item/{itemId}/bookmark")
  suspend fun updateBookmark(
    @Path("itemId") itemId: String,
    @Body request: BookmarkRequest,
  ): Result<Unit>

  @DELETE("/api/me/item/{itemId}/bookmark/{time}")
  suspend fun deleteBookmark(@Path("itemId") itemId: String, @Path("time") time: Int): Result<Unit>

  @PATCH("/api/me/password")
  suspend fun changePassword(@Body request: ChangePasswordRequest): Result<Unit>

  // session

  @POST("/api/session/{sessionId}/sync")
  suspend fun syncSession(
    @Path("sessionId") sessionId: String,
    @Body request: SyncSessionRequest,
  ): Result<Unit>

  @POST("/api/session/local")
  suspend fun syncLocalSession(@Body request: SyncLocalSessionRequest): Result<Unit>

  @POST("/api/session/local-all")
  suspend fun syncLocalAllSession(
    @Body sessions: SyncLocalAllSessionRequest
  ): Result<SyncLocalAllSessionResponse>

  @GET("/api/sessions")
  suspend fun sessions(
    @Query("itemsPerPage") itemsPerPage: Int = 10,
    @Query("page") page: Int = 0,
    @Query("sort") sort: String = "updatedAt",
    @Query("user") user: String? = null,
    @Query("desc") desc: Int = 1,
  ): Result<SessionsResponse>

  @DELETE("/api/sessions/{sessionId}")
  suspend fun deleteSession(@Path("sessionId") sessionId: String): Result<Unit>

  @POST("/api/sessions/batch/delete")
  suspend fun deleteSessions(@Body request: DeleteSessionsRequest): Result<Unit>

  @GET("/api/sessions/open") suspend fun openSessions(): Result<OpenSessionsResponse>

  @POST("/api/session/{sessionId}/close")
  suspend fun closeSession(@Path("sessionId") sessionId: String): Result<Unit>

  // search
  @GET("/api/search/podcast")
  suspend fun searchPodcast(@Query("term") term: String): Result<List<SearchPodcast>>

  // users
  @POST("/api/users")
  suspend fun createUser(@Body request: CreateUserRequest): Result<CreateUserResponse>

  @GET("/api/users")
  suspend fun users(@Query("include") include: String? = "latestSession"): Result<UsersResponse>

  @GET("/api/users/{userId}")
  suspend fun user(@Path("userId") userId: String): Result<UserWithMediaProgressDetail>

  @PATCH("/api/users/{userId}")
  suspend fun updateUser(
    @Path("userId") userId: String,
    @Body request: UpdateUserRequest,
  ): Result<UpdateUserResponse>

  @DELETE("/api/users/{userId}")
  suspend fun deleteUser(@Path("userId") userId: String): Result<DeleteUserResponse>

  @GET("/api/users/{userId}/listening-stats")
  suspend fun listeningStats(@Path("userId") userId: String): Result<ListeningStatResponse>

  // podcasts
  @POST("/api/podcasts/feed")
  suspend fun podcastFeed(@Body request: PodcastFeedRequest): Result<PodcastFeed>

  @POST("/api/podcasts")
  suspend fun createPodcast(@Body request: CreatePodcastRequest): Result<LibraryItem>

  @POST("/api/podcasts/{itemId}/download-episodes")
  suspend fun downloadEpisodes(
    @Path("itemId") itemId: String,
    @Body episodes: List<Episode>,
  ): Result<Unit>

  @DELETE("/api/podcasts/{itemId}/episode/{episodeId}")
  suspend fun deleteEpisode(
    @Path("itemId") itemId: String,
    @Path("episodeId") episodeId: String,
    @Query("hard") hard: Int = 0,
  ): Result<Unit>

  // tags
  @GET("/api/tags") suspend fun tags(): Result<TagsResponse>

  // settings
  @PATCH("/api/settings")
  suspend fun updateSettings(
    @Body request: UpdateServerSettingsRequest
  ): Result<ServerSettingsResponse>

  // search providers
  @GET("/api/search/providers") suspend fun searchProviders(): Result<SearchProvidersResponse>

  // cache
  @POST("/api/cache/purge") suspend fun purgeCache(): Result<Unit>

  @POST("/api/cache/items/purge") suspend fun purgeItemsCache(): Result<Unit>

  // logger
  @GET("/api/logger-data") suspend fun logs(): Result<LogsResponse>
}
