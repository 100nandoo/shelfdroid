package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val libraryRepo: LibraryRepo,
) {

  suspend fun getUser(uiState: HomeUiState): HomeUiState {
    val response = api.me()
    val result = response.getOrNull()

    return if (result != null) {
      progressRepo.saveAndConvert(result)
      bookmarkRepo.saveAndConvert(result)
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
