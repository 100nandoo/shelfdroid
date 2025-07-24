package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.User
import dev.halim.core.network.response.UserType
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
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
    return result.map { LibraryUiState(it.id, it.name) }
  }

  suspend fun getLibraryItems(libraryId: String): List<ShelfdroidMediaItem> {
    val ids = libraryItemRepo.idsByLibraryId(libraryId)
    val progresses = progressRepo.entities()
    val libraryItems = libraryItemRepo.entities(libraryId, ids)
    return libraryItems.map { item ->
      val progress = progresses.firstOrNull { it.libraryItemId == item.id }
      toUiState(item, progress)
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

  private fun toUiState(item: LibraryItemEntity, progress: ProgressEntity?): ShelfdroidMediaItem {
    return if (item.isBook == 1L) {
      val progressValue = progress?.progress?.toFloat() ?: 0f

      BookUiState(
        id = item.id,
        author = item.author,
        title = item.title,
        cover = item.cover,
        progress = progressValue,
      )
    } else {
      PodcastUiState(
        id = item.id,
        author = item.author,
        title = item.title,
        cover = item.cover,
        seekTime = 0,
        episodeCount = 0,
      )
    }
  }
}
