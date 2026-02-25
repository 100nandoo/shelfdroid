package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LoginResponse
import dev.halim.core.network.response.UserType as NetworkUserType
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.response.TagRepo
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.extensions.toBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val libraryRepo: LibraryRepo,
  private val userRepo: UserRepo,
  private val tagRepo: TagRepo,
  private val mapper: HomeMapper,
  private val prefsRepository: PrefsRepository,
  private val appScope: CoroutineScope,
) {

  fun item(): Flow<Pair<Prefs, List<LibraryUiState>>> {
    val libraries = libraryRepo.flowEntities()
    val libraryItems = libraryItemRepo.flowEntities()
    val progresses = progressRepo.flowAll()
    val prefs = prefsRepository.prefsFlow()

    return combine(libraries, libraryItems, prefs, progresses) {
      libraries,
      libraryItems,
      prefs,
      progresses ->
      val result =
        libraries.map { (id, name, _, isBookLibrary) ->
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
    libraryRepo.remote()
    libraryItemRepo.remote()

    backgroundRemoteSync()

    return homeUiState.copy(state = GenericState.Success)
  }

  private fun backgroundRemoteSync() {
    appScope.launch { userRepo.remote() }
    appScope.launch { tagRepo.remote() }
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
    val serverPrefs = ServerPrefs(version = server.version)
    prefsRepository.updateServerPrefs(serverPrefs)
  }
}
