package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.catalog.LibraryItemRepository
import dev.halim.shelfdroid.core.data.catalog.LibraryRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HomeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepository,
  private val progressRepo: ProgressRepository,
  private val libraryRepo: LibraryRepository,
  private val mapper: HomeMapper,
  private val prefsRepository: PrefsRepository,
  private val downloadRepo: DownloadRepo,
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
}
