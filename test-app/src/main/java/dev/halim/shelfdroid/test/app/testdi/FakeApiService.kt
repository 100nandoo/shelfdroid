package dev.halim.shelfdroid.test.app.testdi

import dev.halim.core.network.ApiService
import dev.halim.core.network.LibraryItemsSort
import dev.halim.core.network.client.AnonymousRequestTag
import dev.halim.core.network.request.*
import dev.halim.core.network.request.apprisenotificationsettings.*
import dev.halim.core.network.request.authenticationsettings.*
import dev.halim.core.network.request.emailmanagement.*
import dev.halim.core.network.request.metadata.*
import dev.halim.core.network.response.*
import dev.halim.core.network.response.apprisenotificationsettings.*
import dev.halim.core.network.response.authenticationsettings.*
import dev.halim.core.network.response.emailmanagement.*
import dev.halim.core.network.response.libraryitem.*
import dev.halim.core.network.response.libraryitem.Podcast as LibraryPodcast
import dev.halim.core.network.response.libraryitem.PodcastMetadata as LibraryPodcastMetadata
import dev.halim.core.network.response.login.*
import dev.halim.core.network.response.metadata.*
import dev.halim.core.network.response.play.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result
import okhttp3.MultipartBody

@Singleton
class FakeApiService @Inject constructor() : ApiService {
  private val rootUser =
    User(
      id = ROOT_USER_ID,
      username = "root",
      email = "root@example.com",
      type = UserType.ROOT,
      token = "token-root",
      isActive = true,
      permissions =
        dev.halim.core.network.response.login.Permissions(
          download = true,
          update = true,
          delete = true,
          upload = true,
          createEreader = true,
          accessAllLibraries = true,
          accessAllTags = true,
          accessExplicitContent = true,
          selectedTagsNotAccessible = false,
        ),
      librariesAccessible = listOf(BOOK_LIBRARY_ID, PODCAST_LIBRARY_ID),
      accessToken = "token-root",
      refreshToken = "refresh-root",
    )

  private val appUser =
    User(
      id = APP_USER_ID,
      username = "operator",
      email = "operator@example.com",
      type = UserType.USER,
      token = "token-operator",
      isActive = true,
      permissions =
        dev.halim.core.network.response.login.Permissions(
          download = true,
          update = true,
          delete = false,
          upload = false,
          createEreader = false,
          accessAllLibraries = true,
          accessAllTags = true,
          accessExplicitContent = true,
          selectedTagsNotAccessible = false,
        ),
      librariesAccessible = listOf(BOOK_LIBRARY_ID, PODCAST_LIBRARY_ID),
      accessToken = "token-operator",
      refreshToken = "refresh-operator",
    )

  private val loginResponse =
    LoginResponse(
      user = rootUser,
      userDefaultLibraryId = PODCAST_LIBRARY_ID,
      serverSettings = ServerSettings(version = "1.0.0-test", logLevel = 1),
      source = "test-app",
    )

  private val bookLibrary =
    Library(
      id = BOOK_LIBRARY_ID,
      name = "Books",
      mediaType = MediaType.BOOK,
      folders =
        listOf(dev.halim.core.network.response.Folder(id = "folder-books", fullPath = "/books")),
    )

  private val podcastLibrary =
    Library(
      id = PODCAST_LIBRARY_ID,
      name = "Podcasts",
      mediaType = MediaType.PODCAST,
      folders =
        listOf(
          dev.halim.core.network.response.Folder(
            id = PODCAST_FOLDER_ID,
            fullPath = "/podcasts",
          )
        ),
    )

  private val searchResult =
    SearchPodcast(
      id = 101,
      artistId = 202,
      title = "Testing Podcast",
      artistName = "QA FM",
      description = "A podcast used for instrumentation coverage.",
      descriptionPlain = "A podcast used for instrumentation coverage.",
      releaseDate = "2024-01-01",
      genres = listOf("Technology", "Testing"),
      cover = "https://example.com/testing-podcast.png",
      trackCount = 12,
      feedUrl = "https://example.com/testing-podcast.rss",
      pageUrl = "https://example.com/testing-podcast",
      explicit = false,
    )

  private val podcastFeed =
    PodcastFeed(
      podcast =
        dev.halim.core.network.response.Podcast(
          metadata =
            dev.halim.core.network.response.PodcastMetadata(
              image = searchResult.cover,
              categories = searchResult.genres,
              feedUrl = searchResult.feedUrl,
              description = searchResult.description,
              descriptionPlain = searchResult.descriptionPlain,
              type = "episodic",
              title = searchResult.title,
              language = "en",
              explicit = "false",
              author = searchResult.artistName,
              pubDate = "2024-01-01",
              link = searchResult.pageUrl,
            ),
          episodes =
            listOf(
              Episode(
                title = "Instrumentation",
                subtitle = "",
                description = "Episode used for end-to-end test coverage.",
                descriptionPlain = "Episode used for end-to-end test coverage.",
                pubDate = "2024-01-01",
                episodeType = "full",
                season = "1",
                episode = "1",
                author = searchResult.artistName,
                duration = "600",
                explicit = "false",
                publishedAt = 1_700_000_000_000,
                enclosure =
                  dev.halim.core.network.response.Enclosure(
                    url = "https://example.com/ep1.mp3",
                    type = "audio/mpeg",
                    length = "600",
                  ),
              )
            ),
        )
    )

  private val libraries = listOf(bookLibrary, podcastLibrary)

  private val items = linkedMapOf<String, LibraryItem>()
  private val apiKeys = mutableListOf<ApiKeysResponse.ApiKey>()
  private val users = mutableListOf(rootUser, appUser)
  private val tags = mutableListOf<String>()
  private val genres = mutableListOf<String>()
  private val customMetadataProviders = mutableListOf<CustomMetadataProvider>()
  private var createdPodcastCount = 0
  private var createdApiKeyCount = 0
  private var createdCustomMetadataProviderCount = 0

  init {
    reset()
  }

  fun reset() {
    synchronized(this) {
      items.clear()
      items[BOOK_ITEM_ID] = createBookItem()
      items[PODCAST_ITEM_ID] =
        createPodcastItem(
          id = PODCAST_ITEM_ID,
          title = "Daily Bytes",
          feedUrl = "https://example.com/daily-bytes.rss",
        )

      apiKeys.clear()
      apiKeys +=
        ApiKeysResponse.ApiKey(
          id = API_KEY_ID,
          name = "Primary Key",
          description = null,
          expiresAt = null,
          lastUsedAt = null,
          isActive = true,
          permissions = null,
          createdAt = "2024-01-01T00:00:00Z",
          updatedAt = "2024-01-01T00:00:00Z",
          userId = ROOT_USER_ID,
          createdByUserId = ROOT_USER_ID,
          user = ApiKeysResponse.ApiKey.User(ROOT_USER_ID, "root", "root"),
          createdByUser = ApiKeysResponse.ApiKey.User(ROOT_USER_ID, "root", "root"),
        )
      users.clear()
      users += rootUser
      users += appUser
      tags.clear()
      tags += listOf("fiction", "technology")
      genres.clear()
      genres += listOf("Fantasy", "Technology")
      customMetadataProviders.clear()
      createdPodcastCount = 0
      createdApiKeyCount = 0
      createdCustomMetadataProviderCount = 0
    }
  }

  override suspend fun apiKeys(): Result<ApiKeysResponse> =
    Result.success(ApiKeysResponse(apiKeys = synchronized(this) { apiKeys.toList() }))

  override suspend fun createApiKey(
    request: CreateApiKeyRequest
  ): Result<CreateUpdateApiKeyResponse> {
    synchronized(this) {
      createdApiKeyCount += 1
      val id = "api-created-$createdApiKeyCount"
      val user = users.first { it.id == request.userId }
      val created =
        ApiKeysResponse.ApiKey(
          id = id,
          name = request.name,
          description = null,
          expiresAt = null,
          lastUsedAt = null,
          isActive = request.isActive,
          permissions = null,
          createdAt = "2024-02-01T00:00:00Z",
          updatedAt = "2024-02-01T00:00:00Z",
          userId = user.id,
          createdByUserId = ROOT_USER_ID,
          user = ApiKeysResponse.ApiKey.User(user.id, user.username, user.type.name.lowercase()),
          createdByUser = ApiKeysResponse.ApiKey.User(ROOT_USER_ID, "root", "root"),
        )
      apiKeys += created
      return Result.success(
        CreateUpdateApiKeyResponse(
          apiKey =
            CreateUpdateApiKeyResponse.ApiKey(
              id = id,
              name = request.name,
              userId = user.id,
              isActive = request.isActive,
              createdByUserId = ROOT_USER_ID,
              updatedAt = created.updatedAt,
              createdAt = created.createdAt,
              user = created.user,
              apiKey = "fresh-key",
            )
        )
      )
    }
  }

  override suspend fun updateApiKey(
    apiKeyId: String,
    request: UpdateApiKeyRequest,
  ): Result<CreateUpdateApiKeyResponse> {
    synchronized(this) {
      val index = apiKeys.indexOfFirst { it.id == apiKeyId }
      val current = apiKeys[index]
      val user = users.first { it.id == request.userId }
      val updated =
        current.copy(
          isActive = request.isActive,
          userId = user.id,
          user = ApiKeysResponse.ApiKey.User(user.id, user.username, user.type.name.lowercase()),
          updatedAt = "2024-02-02T00:00:00Z",
        )
      apiKeys[index] = updated
      return Result.success(
        CreateUpdateApiKeyResponse(
          apiKey =
            CreateUpdateApiKeyResponse.ApiKey(
              id = updated.id,
              name = updated.name,
              userId = updated.userId,
              isActive = updated.isActive,
              createdByUserId = updated.createdByUserId,
              updatedAt = updated.updatedAt,
              createdAt = updated.createdAt,
              user = updated.user,
            )
        )
      )
    }
  }

  override suspend fun deleteApiKey(apiKeyId: String): Result<Unit> {
    synchronized(this) { apiKeys.removeAll { it.id == apiKeyId } }
    return Result.success(Unit)
  }

  override suspend fun login(
    request: LoginRequest,
    returnTokens: String,
  ): Result<LoginResponse> = Result.success(loginResponse)

  override suspend fun authorize(returnTokens: String): Result<LoginResponse> =
    Result.success(loginResponse)

  override suspend fun refresh(refreshToken: String): Result<LoginResponse> =
    Result.success(loginResponse)

  override suspend fun logout(refreshToken: String): Result<LogoutResponse> =
    Result.success(LogoutResponse())

  override suspend fun openIdCallback(
    url: String,
    state: String,
    code: String,
    codeVerifier: String,
    anonymousRequestTag: AnonymousRequestTag,
  ): Result<LoginResponse> = Result.success(loginResponse)

  override suspend fun status(
    url: String,
    anonymousRequestTag: AnonymousRequestTag,
  ): Result<LoginStatusResponse> =
    Result.success(
      LoginStatusResponse(
        app = "shelf-droid-test",
        serverVersion = "1.0.0-test",
        isInit = true,
        authMethods = listOf("password"),
      )
    )

  override suspend fun backups(): Result<BackupsResponse> =
    Result.success(BackupsResponse(backups = emptyList(), backupLocation = "/tmp"))

  override suspend fun createBackup(): Result<BackupsResponse> = backups()

  override suspend fun deleteBackup(backupId: String): Result<BackupsResponse> = backups()

  override suspend fun applyBackup(backupId: String): Result<Unit> = Result.success(Unit)

  override suspend fun uploadBackup(file: MultipartBody.Part): Result<Unit> = Result.success(Unit)

  override suspend fun libraries(): Result<LibrariesResponse> =
    Result.success(LibrariesResponse(libraries))

  override suspend fun createLibrary(request: CreateLibraryRequest): Result<Library> =
    Result.success(
      Library(
        id = "created-${request.name.lowercase().replace(' ', '-')}",
        name = request.name,
        mediaType = if (request.mediaType == "podcast") MediaType.PODCAST else MediaType.BOOK,
        icon = request.icon,
        provider = request.provider,
        folders = request.folders.mapIndexed { index, folder ->
          Folder(id = "created-folder-$index", fullPath = folder.path)
        },
      )
    )

  override suspend fun reorderLibraries(
    request: List<ReorderLibraryRequest>
  ): Result<LibrariesResponse> {
    val byId = request.associateBy { it.id }
    val reordered =
      libraries
        .map { library ->
          val newOrder = byId[library.id]?.newOrder ?: library.displayOrder
          library.copy(displayOrder = newOrder)
        }
        .sortedBy { it.displayOrder }
    return Result.success(LibrariesResponse(reordered))
  }

  override suspend fun scanLibrary(libraryId: String): Result<Unit> = Result.success(Unit)

  override suspend fun matchLibrary(libraryId: String): Result<Unit> = Result.success(Unit)

  override suspend fun tasks(): Result<TasksResponse> = Result.success(TasksResponse())

  override suspend fun filesystem(
    path: String?,
    level: Int,
  ): Result<FileSystemResponse> =
    Result.success(
      FileSystemResponse(
        posix = true,
        directories =
          listOf(
            FileSystemDirectory(path = "/books", dirname = "books", level = level),
            FileSystemDirectory(path = "/podcasts", dirname = "podcasts", level = level),
          ),
      )
    )

  override suspend fun libraryItems(
    libraryId: String,
    limit: Int?,
    page: Int?,
    minified: Int?,
    sort: LibraryItemsSort?,
    desc: Int?,
  ): Result<LibraryItemsResponse> {
    val results = synchronized(this) { items.values.filter { it.libraryId == libraryId } }
    return Result.success(LibraryItemsResponse(results = results, total = results.size))
  }

  override suspend fun librarySeries(
    libraryId: String,
    limit: Int,
    page: Int,
    sort: String,
    desc: Int,
  ): Result<LibrarySeriesResponse> = Result.success(LibrarySeriesResponse())

  override suspend fun item(itemId: String, expanded: Int, include: String?): Result<LibraryItem> =
    Result.success(checkNotNull(synchronized(this) { items[itemId] }))

  override suspend fun batchLibraryItems(
    request: BatchLibraryItemsRequest
  ): Result<BatchLibraryItemsResponse> {
    val result = synchronized(this) { request.libraryItemIds.mapNotNull(items::get) }
    return Result.success(BatchLibraryItemsResponse(libraryItems = result))
  }

  override suspend fun playBook(itemId: String, request: PlayRequest): PlayResponse =
    error("Playback is not used by test-app navigation tests")

  override suspend fun playPodcast(
    itemId: String,
    episodeId: String,
    request: PlayRequest,
  ): PlayResponse = error("Playback is not used by test-app navigation tests")

  override suspend fun deleteItem(itemId: String, hard: Int): Result<Unit> {
    synchronized(this) { items.remove(itemId) }
    return Result.success(Unit)
  }

  override suspend fun deleteItemFile(itemId: String, ino: String): Result<Unit> =
    Result.success(Unit)

  override suspend fun updateItemMedia(
    itemId: String,
    request: UpdateLibraryItemMediaRequest,
  ): Result<UpdateLibraryItemMediaResponse> = Result.success(UpdateLibraryItemMediaResponse())

  override suspend fun matchItem(
    itemId: String,
    request: MatchLibraryItemRequest,
  ): Result<MatchItemResult> = Result.failure(UnsupportedOperationException("Unused in test-app"))

  override suspend fun reScanItem(itemId: String): Result<Unit> = Result.success(Unit)

  override suspend fun uploadItemCover(
    itemId: String,
    cover: MultipartBody.Part,
  ): Result<SetItemCoverResponse> = Result.success(SetItemCoverResponse(success = true))

  override suspend fun setItemCoverFromUrl(
    itemId: String,
    request: CoverFromUrlRequest,
  ): Result<SetItemCoverResponse> =
    Result.failure(UnsupportedOperationException("Unused in test-app"))

  override suspend fun deleteItemCover(itemId: String): Result<Unit> = Result.success(Unit)

  override suspend fun embedItemMetadata(itemId: String): Result<Unit> = Result.success(Unit)

  override suspend fun validateCron(request: ValidateCronRequest): Result<Unit> =
    Result.success(Unit)

  override suspend fun searchBooks(
    provider: String,
    title: String,
    author: String?,
    fallbackTitleOnly: Int?,
    id: String?,
  ): Result<List<SearchBookMatchResponse>> = Result.success(emptyList())

  override suspend fun searchCovers(
    title: String,
    author: String?,
    provider: String?,
    podcast: Int,
  ): Result<SearchCoversResponse> =
    Result.failure(UnsupportedOperationException("Unused in test-app"))

  override suspend fun me(): Result<User> = Result.success(rootUser)

  override suspend fun patchBookProgress(itemId: String, request: ProgressRequest): Result<Unit> =
    Result.success(Unit)

  override suspend fun patchPodcastProgress(
    itemId: String,
    episodeId: String,
    request: ProgressRequest,
  ): Result<Unit> = Result.success(Unit)

  override suspend fun createBookmark(
    itemId: String,
    request: BookmarkRequest,
  ): Result<AudioBookmark> =
    Result.success(AudioBookmark(libraryItemId = itemId, title = request.title))

  override suspend fun updateBookmark(
    itemId: String,
    request: BookmarkRequest,
  ): Result<Unit> = Result.success(Unit)

  override suspend fun deleteBookmark(itemId: String, time: Int): Result<Unit> =
    Result.success(Unit)

  override suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> =
    Result.success(Unit)

  override suspend fun syncSession(
    sessionId: String,
    request: SyncSessionRequest,
  ): Result<Unit> = Result.success(Unit)

  override suspend fun syncLocalSession(request: SyncLocalSessionRequest): Result<Unit> =
    Result.success(Unit)

  override suspend fun syncLocalAllSession(
    sessions: SyncLocalAllSessionRequest
  ): Result<SyncLocalAllSessionResponse> = Result.success(SyncLocalAllSessionResponse(emptyList()))

  override suspend fun sessions(
    itemsPerPage: Int,
    page: Int,
    sort: String,
    user: String?,
    desc: Int,
  ): Result<dev.halim.core.network.response.SessionsResponse> =
    Result.success(
      dev.halim.core.network.response.SessionsResponse(
        total = 0,
        numPages = 0,
        page = 0,
        itemsPerPage = itemsPerPage,
        sessions = emptyList(),
      )
    )

  override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.success(Unit)

  override suspend fun deleteSessions(request: DeleteSessionsRequest): Result<Unit> =
    Result.success(Unit)

  override suspend fun openSessions(): Result<OpenSessionsResponse> =
    Result.success(OpenSessionsResponse(sessions = emptyList()))

  override suspend fun closeSession(sessionId: String): Result<Unit> = Result.success(Unit)

  override suspend fun searchPodcast(term: String, provider: String?): Result<List<SearchPodcast>> =
    Result.success(listOf(searchResult))

  override suspend fun searchPodcastEpisode(
    itemId: String,
    title: String,
  ): Result<SearchPodcastEpisodeResponse> = Result.success(SearchPodcastEpisodeResponse())

  override suspend fun createUser(request: CreateUserRequest): Result<CreateUserResponse> {
    val user =
      User(
        id = "user-created",
        username = request.username,
        email = request.email,
        type = UserType.USER,
        isActive = request.isActive,
        permissions = rootUser.permissions,
      )
    synchronized(this) { users += user }
    return Result.success(CreateUserResponse(user))
  }

  override suspend fun users(include: String?): Result<UsersResponse> =
    Result.success(UsersResponse(users = synchronized(this) { users.toList() }))

  override suspend fun user(userId: String): Result<UserWithMediaProgressDetail> {
    val user = synchronized(this) { users.first { it.id == userId } }
    return Result.success(
      UserWithMediaProgressDetail(
        id = user.id,
        username = user.username,
        email = user.email,
        type = user.type.name.lowercase(),
        token = user.token,
        mediaProgress = emptyList(),
        seriesHideFromContinueListening = emptyList(),
        bookmarks = emptyList(),
        isActive = user.isActive,
        isLocked = false,
        lastSeen = 1_700_000_000_000,
        createdAt = 1_700_000_000_000,
        permissions =
          UserWithMediaProgressDetail.Permissions(
            download = user.permissions.download,
            update = user.permissions.update,
            delete = user.permissions.delete,
            upload = user.permissions.upload,
            accessAllLibraries = user.permissions.accessAllLibraries,
            accessAllTags = user.permissions.accessAllTags,
            accessExplicitContent = user.permissions.accessExplicitContent,
          ),
        librariesAccessible = user.librariesAccessible,
        itemTagsSelected = user.itemTagsSelected,
        hasOpenIDLink = false,
      )
    )
  }

  override suspend fun updateUser(
    userId: String,
    request: UpdateUserRequest,
  ): Result<UpdateUserResponse> =
    Result.failure(UnsupportedOperationException("Unused in test-app"))

  override suspend fun deleteUser(userId: String): Result<DeleteUserResponse> {
    synchronized(this) { users.removeAll { it.id == userId } }
    return Result.success(DeleteUserResponse(success = true))
  }

  override suspend fun listeningStats(userId: String): Result<ListeningStatResponse> =
    Result.success(
      ListeningStatResponse(
        totalTime = 7_200.0,
        today = 300.0,
        days = mapOf("2024-01-01" to 300.0),
        dayOfWeek = mapOf("Mon" to 300.0),
        recentSessions =
          listOf(
            Session(
              id = "session-1",
              userId = ROOT_USER_ID,
              libraryId = PODCAST_LIBRARY_ID,
              libraryItemId = PODCAST_ITEM_ID,
              bookId = null,
              episodeId = PODCAST_EPISODE_ID,
              mediaType = "podcast",
              mediaMetadata = createPodcastMedia("Daily Bytes").metadata,
              chapters = emptyList(),
              displayTitle = "Daily Bytes",
              displayAuthor = "Shelf Labs",
              coverPath = "",
              duration = 600.0,
              playMethod = 0,
              mediaPlayer = "media3",
              deviceInfo =
                dev.halim.core.network.response.DeviceInfo(
                  clientName = "test-app",
                  clientVersion = "1.0.0",
                ),
              date = "2024-01-01",
              dayOfWeek = "Mon",
              timeListening = 300.0,
              startTime = 0.0,
              currentTime = 300.0,
              startedAt = 1_700_000_000_000,
              updatedAt = 1_700_000_000_300,
              user = SessionUser(ROOT_USER_ID, "root"),
            )
          ),
      )
    )

  override suspend fun podcastFeed(request: PodcastFeedRequest): Result<PodcastFeed> =
    Result.success(podcastFeed)

  override suspend fun createPodcast(request: CreatePodcastRequest): Result<LibraryItem> {
    synchronized(this) {
      createdPodcastCount += 1
      val item =
        createPodcastItem(
          id = "podcast-created-$createdPodcastCount",
          title = request.media.metadata.title ?: "New Podcast",
          feedUrl = request.media.metadata.feedUrl ?: searchResult.feedUrl,
        )
      items[item.id] = item
      return Result.success(item)
    }
  }

  override suspend fun downloadEpisodes(itemId: String, episodes: List<Episode>): Result<Unit> =
    Result.success(Unit)

  override suspend fun deleteEpisode(
    itemId: String,
    episodeId: String,
    hard: Int,
  ): Result<Unit> = Result.success(Unit)

  override suspend fun checkNewEpisodes(
    itemId: String,
    limit: Int,
  ): Result<CheckNewEpisodesResponse> = Result.success(CheckNewEpisodesResponse())

  override suspend fun podcastEpisode(
    itemId: String,
    episodeId: String,
  ): Result<PodcastEpisode> = Result.success(PodcastEpisode())

  override suspend fun updatePodcastEpisode(
    itemId: String,
    episodeId: String,
    request: UpdatePodcastEpisodeRequest,
  ): Result<LibraryItem> = item(itemId)

  override suspend fun tags(): Result<TagsResponse> =
    Result.success(TagsResponse(tags = synchronized(this) { tags.toList() }))

  override suspend fun renameTag(request: RenameTagRequest): Result<TagMutationResponse> {
    synchronized(this) {
      val updated = tags.count { it == request.tag }
      if (tags.contains(request.newTag).not()) {
        tags.replaceAll { if (it == request.tag) request.newTag else it }
      } else {
        tags.removeAll { it == request.tag }
      }
      return Result.success(
        TagMutationResponse(
          numItemsUpdated = updated,
          tagMerged = tags.any { it == request.newTag } && request.tag != request.newTag,
        )
      )
    }
  }

  override suspend fun deleteTag(tag: String): Result<TagMutationResponse> {
    val decoded = runCatching {
      val base64 = URLDecoder.decode(tag, StandardCharsets.UTF_8)
      String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)
    }
      .getOrElse {
        return Result.failure(it)
      }
    synchronized(this) {
      val updated = tags.count { it == decoded }
      tags.removeAll { it == decoded }
      return Result.success(TagMutationResponse(numItemsUpdated = updated))
    }
  }

  override suspend fun genres(): Result<GenresResponse> =
    Result.success(GenresResponse(genres = synchronized(this) { genres.toList() }))

  override suspend fun renameGenre(request: RenameGenreRequest): Result<GenreMutationResponse> {
    synchronized(this) {
      val updated = genres.count { it == request.genre }
      val merged = genres.any { it == request.newGenre && it != request.genre }
      if (merged) {
        genres.removeAll { it == request.genre }
      } else {
        genres.replaceAll { if (it == request.genre) request.newGenre else it }
      }
      return Result.success(GenreMutationResponse(numItemsUpdated = updated, genreMerged = merged))
    }
  }

  override suspend fun deleteGenre(genre: String): Result<GenreMutationResponse> {
    val decoded = runCatching {
      val base64 = URLDecoder.decode(genre, StandardCharsets.UTF_8)
      String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)
    }
      .getOrElse {
        return Result.failure(it)
      }
    synchronized(this) {
      val updated = genres.count { it == decoded }
      genres.removeAll { it == decoded }
      return Result.success(GenreMutationResponse(numItemsUpdated = updated))
    }
  }

  override suspend fun customMetadataProviders(): Result<CustomMetadataProvidersResponse> =
    Result.success(
      CustomMetadataProvidersResponse(
        providers = synchronized(this) { customMetadataProviders.toList() }
      )
    )

  override suspend fun createCustomMetadataProvider(
    request: CreateCustomMetadataProviderRequest
  ): Result<CustomMetadataProviderResponse> {
    if (request.name.isBlank() || request.url.isBlank()) {
      return Result.failure(IllegalArgumentException("Invalid custom metadata provider"))
    }
    synchronized(this) {
      createdCustomMetadataProviderCount += 1
      val provider =
        CustomMetadataProvider(
          id = "custom-provider-$createdCustomMetadataProviderCount",
          name = request.name,
          url = request.url,
          mediaType = request.mediaType,
          slug = "custom-custom-provider-$createdCustomMetadataProviderCount",
          authHeaderValue = request.authHeaderValue,
        )
      customMetadataProviders += provider
      return Result.success(CustomMetadataProviderResponse(provider = provider))
    }
  }

  override suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> {
    synchronized(this) {
      if (customMetadataProviders.removeAll { it.id == providerId }) return Result.success(Unit)
      return Result.failure(NoSuchElementException("Custom metadata provider not found"))
    }
  }

  override suspend fun updateSettings(
    request: UpdateServerSettingsRequest
  ): Result<ServerSettingsResponse> =
    Result.success(ServerSettingsResponse(serverSettings = loginResponse.serverSettings))

  override suspend fun authenticationSettings(): Result<AuthenticationSettingsResponse> =
    Result.success(AuthenticationSettingsResponse(authActiveAuthMethods = listOf("password")))

  override suspend fun updateAuthenticationSettings(
    request: UpdateAuthenticationSettingsRequest
  ): Result<UpdateAuthenticationSettingsResponse> =
    Result.success(UpdateAuthenticationSettingsResponse(updated = true))

  override suspend fun openIdIssuerConfiguration(
    issuer: String
  ): Result<OpenIdIssuerConfigurationResponse> = Result.success(OpenIdIssuerConfigurationResponse())

  override suspend fun appriseNotificationSettings(): Result<AppriseNotificationSettingsResponse> =
    Result.success(AppriseNotificationSettingsResponse())

  override suspend fun updateAppriseNotificationSettings(
    request: UpdateAppriseNotificationSettingsRequest
  ): Result<Unit> = Result.success(Unit)

  override suspend fun createAppriseNotificationRule(
    request: AppriseNotificationRuleRequest
  ): Result<AppriseNotificationSettings> = Result.success(AppriseNotificationSettings())

  override suspend fun updateAppriseNotificationRule(
    ruleId: String,
    request: AppriseNotificationRuleRequest,
  ): Result<AppriseNotificationSettings> = Result.success(AppriseNotificationSettings())

  override suspend fun updateAppriseNotificationRule(
    ruleId: String,
    request: NotificationRuleEnabledRequest,
  ): Result<AppriseNotificationSettings> = Result.success(AppriseNotificationSettings())

  override suspend fun deleteAppriseNotificationRule(
    ruleId: String
  ): Result<AppriseNotificationSettings> = Result.success(AppriseNotificationSettings())

  override suspend fun testAppriseNotificationRule(ruleId: String): Result<Unit> =
    Result.success(Unit)

  override suspend fun emailSettings(): Result<EmailSettingsResponse> =
    Result.success(EmailSettingsResponse())

  override suspend fun updateEmailSettings(
    request: UpdateEmailSettingsRequest
  ): Result<EmailSettingsResponse> = Result.success(EmailSettingsResponse())

  override suspend fun sendTestEmail(): Result<Unit> = Result.success(Unit)

  override suspend fun updateEreaderDevices(
    request: UpdateEreaderDevicesRequest
  ): Result<EreaderDevicesResponse> = Result.success(EreaderDevicesResponse())

  override suspend fun searchProviders(): Result<SearchProvidersResponse> =
    Result.failure(UnsupportedOperationException("Unused in test-app"))

  override suspend fun purgeCache(): Result<Unit> = Result.success(Unit)

  override suspend fun purgeItemsCache(): Result<Unit> = Result.success(Unit)

  override suspend fun rssFeeds(): Result<RssFeedsResponse> = Result.success(RssFeedsResponse())

  override suspend fun openItemRssFeed(
    itemId: String,
    request: OpenItemRssFeedRequest,
  ): Result<OpenItemRssFeedResponse> =
    Result.success(OpenItemRssFeedResponse(feed = RssFeed(id = "rss-test")))

  override suspend fun closeRssFeed(feedId: String): Result<Unit> = Result.success(Unit)

  override suspend fun logs(): Result<LogsResponse> =
    Result.success(
      LogsResponse(
        currentDailyLogs =
          listOf(
            LogsResponse.CurrentDailyLog(
              timestamp = "2024-01-01T00:00:00Z",
              source = "test-app",
              message = "fake log",
              levelName = "INFO",
              level = 1,
            )
          )
      )
    )

  private fun createBookItem(): LibraryItem =
    LibraryItem(
      id = BOOK_ITEM_ID,
      libraryId = BOOK_LIBRARY_ID,
      mediaType = "book",
      updatedAt = 1_700_000_000_000,
      addedAt = 1_700_000_000_000,
      media =
        Book(
          libraryItemId = BOOK_ITEM_ID,
          metadata =
            BookMetadata(
              title = "Systems Book",
              description = "A durable test audiobook.",
              authors = listOf(Author(name = "Shelf Droid")),
              narrators = listOf("Narrator"),
              genres = listOf("Technology"),
              language = "en",
            ),
          audioFiles = listOf(AudioFile(ino = "book-ino-1")),
          audioTracks = listOf(AudioTrack(index = 0, title = "Systems Book")),
          chapters = listOf(BookChapter(id = 1, title = "Chapter 1", end = 60.0)),
          duration = 3_600.0,
        ),
    )

  private fun createPodcastItem(id: String, title: String, feedUrl: String): LibraryItem =
    LibraryItem(
      id = id,
      libraryId = PODCAST_LIBRARY_ID,
      mediaType = "podcast",
      updatedAt = 1_700_000_100_000,
      addedAt = 1_700_000_100_000,
      media = createPodcastMedia(title = title, feedUrl = feedUrl),
    )

  private fun createPodcastMedia(
    title: String,
    feedUrl: String = searchResult.feedUrl,
  ): LibraryPodcast =
    LibraryPodcast(
      libraryItemId = PODCAST_ITEM_ID,
      coverPath = "",
      tags = listOf("technology"),
      metadata =
        LibraryPodcastMetadata(
          title = title,
          author = "Shelf Labs",
          description = "A podcast used in instrumentation tests.",
          genres = listOf("Technology"),
          language = "en",
          feedUrl = feedUrl,
          explicit = false,
          itunesId = searchResult.id.toString(),
        ),
      episodes =
        listOf(
          PodcastEpisode(
            libraryItemId = PODCAST_ITEM_ID,
            id = PODCAST_EPISODE_ID,
            title = "Episode 1",
            description = "Episode 1 description",
            audioTrack = AudioTrack(index = 0, title = "Episode 1"),
          )
        ),
    )

  companion object {
    const val ROOT_USER_ID = "user-root"
    const val APP_USER_ID = "user-app"
    const val BOOK_LIBRARY_ID = "lib-book"
    const val PODCAST_LIBRARY_ID = "lib-podcast"
    const val PODCAST_FOLDER_ID = "folder-podcast"
    const val BOOK_ITEM_ID = "item-book"
    const val PODCAST_ITEM_ID = "item-podcast"
    const val PODCAST_EPISODE_ID = "episode-1"
    const val API_KEY_ID = "api-key-1"
  }
}
