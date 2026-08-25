package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.GenericState
import kotlinx.serialization.Serializable

data class HomeUiState(
  val state: GenericState = GenericState.Loading,
  val prefs: Prefs = Prefs(),
  val currentPage: Int = 0,
  /** The last Library selected by the user, retained while the pager is on Misc/admin pages. */
  val activeLibraryId: String? = null,
  val librariesUiState: List<LibraryUiState> = emptyList(),
)

data class LibraryUiState(
  val id: String = "",
  val name: String = "",
  val isBookLibrary: Boolean = true,
  val books: List<BookUiState> = emptyList(),
  val podcasts: List<PodcastUiState> = emptyList(),
)

@Serializable
data class BookUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val duration: Double = 0.0,
  val addedAt: Long = 0,
  val isDownloaded: Boolean = false,
  val trackIndexes: List<Int> = emptyList(),
  val progressLastUpdate: Long = 0,
)

data class PodcastUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val addedAt: Long = 0,
  val progressLastUpdate: Long = 0,
  val episodeCount: Int = 0,
  val unfinishedCount: Int = 0,
  val downloadedCount: Int = 0,
  val unfinishedAndDownloadCount: Int = 0,
)

/**
 * Reconciles Home's selected Library after the catalog changes.
 *
 * Selection is ID-based so navigating through Misc (including Library administration) does not
 * accidentally change the selected Library when an unrelated Library is removed. If the selected
 * Library is removed, the item that takes its old ordered position is selected; when it was the
 * final item, the preceding Library is selected instead.
 */
fun reconcileActiveLibraryId(
  previousLibraries: List<LibraryUiState>,
  activeLibraryId: String?,
  updatedLibraries: List<LibraryUiState>,
): String? {
  if (updatedLibraries.isEmpty()) return null

  if (activeLibraryId != null && updatedLibraries.any { it.id == activeLibraryId }) {
    return activeLibraryId
  }

  val previousIndex = previousLibraries.indexOfFirst { it.id == activeLibraryId }
  if (previousIndex < 0) return updatedLibraries.first().id

  return updatedLibraries.getOrNull(previousIndex)?.id ?: updatedLibraries.last().id
}

sealed interface HomeState {
  data object Loading : HomeState

  data object Success : HomeState

  data class Failure(val errorMessage: String?) : HomeState
}
