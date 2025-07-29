package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.User
import dev.halim.core.network.response.UserType
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val libraryRepo: LibraryRepo,
  private val dataStoreManager: DataStoreManager,
) {

  suspend fun getUser(uiState: HomeUiState): HomeUiState {
    val response = api.me()
    val result = response.getOrNull()

    return if (result != null) {
      progressRepo.saveAndConvert(result)
      bookmarkRepo.saveAndConvert(result)
      updateDataStore(result)
      uiState.copy(homeState = HomeState.Success)
    } else {
      uiState.copy(homeState = HomeState.Failure("Get User Failed"))
    }
  }

  suspend fun getLibraries(): List<LibraryUiState> {
    val result = libraryRepo.entities()
    return result.map { LibraryUiState(it.id, it.name, isBook = it.isBook == 1L) }
  }

  suspend fun getLibraryItems(uiState: LibraryUiState): LibraryUiState {
    val libraryId = uiState.id
    val ids = libraryItemRepo.idsByLibraryId(libraryId)
    val libraryItems = libraryItemRepo.entities(libraryId, ids)
    return if (uiState.isBook) {
      val books = libraryItems.map { toBookUiState(it) }
      uiState.copy(books = books)
    } else {
      val podcasts = libraryItems.map { toPodcastUiState(it) }
      uiState.copy(podcasts = podcasts)
    }
  }

  private suspend fun updateDataStore(user: User) {
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
  }

  private fun toBookUiState(item: LibraryItemEntity): BookUiState {
    return BookUiState(id = item.id, author = item.author, title = item.title, cover = item.cover)
  }

  private fun toPodcastUiState(item: LibraryItemEntity): PodcastUiState {
    val podcast = Json.decodeFromString<Podcast>(item.media).episodes.count()
    val finished = progressRepo.byLibraryItemId(item.id).count { it.isFinished == 1L }
    val unfinished = podcast - finished
    return PodcastUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      unfinishedEpisodeCount = unfinished,
    )
  }
}
