package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.login.LoginResponse
import dev.halim.core.network.response.login.UserType as NetworkUserType
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.catalog.LibraryItemRepository
import dev.halim.shelfdroid.core.data.catalog.LibraryRepository
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ListeningStatsRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.core.data.users.UserRepository
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepository,
  private val progressRepo: ProgressRepository,
  private val bookmarkRepo: BookmarkRepository,
  private val libraryRepo: LibraryRepository,
  private val userRepo: UserRepository,
  private val tagRepo: TagRepository,
  private val listeningStatRepo: ListeningStatsRepository,
  private val mapper: HomeMapper,
  private val prefsRepository: PrefsRepository,
  private val downloadRepo: DownloadRepo,
  @Named("io") private val ioScope: CoroutineScope,
) {

  fun item(): Flow<Pair<Prefs, List<LibraryUiState>>> {
    val libraries = libraryRepo.observeLibraries()
    val libraryItems = libraryItemRepo.observeLibraryItemCatalog()
    val progresses = progressRepo.observeAllProgress()
    val prefs = prefsRepository.prefsFlow()
    val downloads = downloadRepo.completedDownloads
    val downloadSignals = combine(downloads, downloadRepo.durableDownloads) { _, _ -> Unit }

    return combine(libraries, libraryItems, prefs, progresses, downloadSignals) {
      libraries,
      libraryItems,
      prefs,
      _,
      _ ->
      val result = libraries.map { (id, name, _, isBookLibrary) ->
        val isBook = isBookLibrary.toBoolean()
        val libraryItems = libraryItems.getOrDefault(id, emptyList())

        val library =
          if (isBook) {
            val books = libraryItems.map { mapper.toBookUiState(it) }
            LibraryUiState(id, name, true, books = books)
          } else {
            val podcasts = libraryItems.map { mapper.toPodcastUiState(it) }
            LibraryUiState(id, name, false, podcasts = podcasts)
          }
        library
      }
      prefs to result
    }
  }

  suspend fun remoteSync(homeUiState: HomeUiState, fromLogin: Boolean = false): HomeUiState {
    if (fromLogin.not()) {
      getUser()
    }
    libraryRepo.refreshLibraries()
    libraryItemRepo.refreshLibraryItems()

    backgroundRefresh()

    return homeUiState.copy(state = GenericState.Success)
  }

  private fun backgroundRefresh() {
    ioScope.launch { listeningStatRepo.refreshListeningStats() }
    ioScope.launch { userRepo.refreshUsers() }
    ioScope.launch { tagRepo.refreshTags() }
  }

  suspend fun getUser() {
    val response = api.authorize()
    val result = response.getOrNull()
    val user = result?.user

    if (user != null) {
      progressRepo.replaceUserProgress(user)
      bookmarkRepo.replaceUserBookmarks(user)
      updateDataStore(result)
    }
  }

  suspend fun deleteItem(
    state: HomeUiState,
    libraryId: String,
    itemId: String,
    isBook: Boolean,
    hardDelete: Boolean,
  ): HomeUiState {
    val hard = if (hardDelete) 1 else 0
    val result = api.deleteItem(itemId = itemId, hard = hard)

    if (!result.isSuccess) {
      return state
    }

    val updatedLibraries =
      state.librariesUiState.map { library ->
        if (library.id != libraryId) return@map library

        if (isBook) {
          library.copy(books = library.books.filterNot { it.id == itemId })
        } else {
          library.copy(podcasts = library.podcasts.filterNot { it.id == itemId })
        }
      }

    libraryItemRepo.cleanupItem(itemId)
    return state.copy(librariesUiState = updatedLibraries)
  }

  private suspend fun updateDataStore(loginResponse: LoginResponse) {
    val user = loginResponse.user
    val old = prefsRepository.userPrefs.firstOrNull()?.copy()
    old?.let {
      val userPrefs =
        UserPrefs(
          id = user.id,
          username = user.username,
          type = UserType.toUserType(user.type.name),
          isAdmin = user.type == NetworkUserType.ADMIN || user.type == NetworkUserType.ROOT,
          download = user.permissions.download,
          upload = user.permissions.upload,
          delete = user.permissions.delete,
          update = user.permissions.update,
          accessToken = old.accessToken,
          refreshToken = old.refreshToken,
        )
      prefsRepository.updateUserPrefs(userPrefs)
    }

    val server = loginResponse.serverSettings
    prefsRepository.updateServerPrefs(server)
  }
}
