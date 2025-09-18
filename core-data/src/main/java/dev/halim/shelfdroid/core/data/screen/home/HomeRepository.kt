package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.UserType
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
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

  suspend fun getUser(uiState: HomeUiState): HomeUiState {
    val response = api.authorize()
    val result = response.getOrNull()
    val user = result?.user

    return if (user != null) {
      progressRepo.saveAndConvert(user)
      bookmarkRepo.saveAndConvert(user)
      updateDataStore(result)
      uiState.copy(homeState = HomeState.Success)
    } else {
      uiState.copy(homeState = HomeState.Failure("Get User Failed"))
    }
  }

  suspend fun getLibraries(): List<LibraryUiState> {
    val result = libraryRepo.entities()
    return result.map { LibraryUiState(it.id, it.name, isBookLibrary = it.isBookLibrary == 1L) }
  }

  suspend fun getLibraryItems(uiState: LibraryUiState): LibraryUiState {
    val libraryId = uiState.id
    val ids = libraryItemRepo.idsByLibraryId(libraryId)
    val libraryItems = libraryItemRepo.entities(libraryId, ids)
    return if (uiState.isBookLibrary) {
      val books = libraryItems.map { mapper.toBookUiState(it) }
      uiState.copy(books = books)
    } else {
      val podcasts = libraryItems.map { mapper.toPodcastUiState(it) }
      uiState.copy(podcasts = podcasts)
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
