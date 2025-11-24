package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.UserType
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val libraryRepo: LibraryRepo,
  private val mapper: HomeMapper,
  private val dataStoreManager: DataStoreManager,
) {

  fun item(): Flow<Pair<DisplayPrefs, List<LibraryUiState>>> {
    val libraries = libraryRepo.flowEntities()
    val libraryItems = libraryItemRepo.flowEntities()
    val displayPrefs = dataStoreManager.displayPrefs
    val progresses = progressRepo.flowAll()

    return combine(libraries, libraryItems, displayPrefs, progresses) {
      libraries,
      libraryItems,
      displayPrefs,
      progresses ->
      val result =
        libraries.map { (id, name, _, isBookLibrary) ->
          val isBook = isBookLibrary == 1L
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
      displayPrefs to result
    }
  }

  suspend fun remoteSync(homeUiState: HomeUiState, fromLogin: Boolean = false): HomeUiState {
    if (fromLogin.not()) {
      getUser()
    }
    libraryRepo.remote()
    libraryItemRepo.remote()
    return homeUiState.copy(homeState = HomeState.Success)
  }

  suspend fun getUser() {
    val response = api.authorize()
    val result = response.getOrNull()
    val user = result?.user

    if (user != null) {
      progressRepo.saveAndConvert(user)
      bookmarkRepo.saveAndConvert(user)
      updateDataStore(result)
    }
  }

  private suspend fun updateDataStore(loginResponse: LoginResponse) {
    val user = loginResponse.user
    val old = dataStoreManager.userPrefs.firstOrNull()?.copy()
    old?.let {
      val userPrefs =
        UserPrefs(
          id = user.id,
          username = user.username,
          isAdmin = user.type == UserType.ADMIN || user.type == UserType.ROOT,
          download = user.permissions.download,
          upload = user.permissions.upload,
          delete = user.permissions.delete,
          update = user.permissions.update,
          accessToken = old.accessToken,
          refreshToken = old.refreshToken,
        )
      dataStoreManager.updateUserPrefs(userPrefs)
    }

    val server = loginResponse.serverSettings
    val serverPrefs = ServerPrefs(version = server.version)
    dataStoreManager.updateServerPrefs(serverPrefs)
  }
}
