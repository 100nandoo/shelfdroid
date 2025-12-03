package dev.halim.shelfdroid.core.ui.screen.home

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@UnstableApi
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: HomeRepository,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  val fromLogin: Boolean = checkNotNull(savedStateHandle.get<Boolean>("fromLogin"))

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> =
    combine(_uiState, repository.item()) { state, (prefs, libraries) ->
        state.copy(prefs = prefs, librariesUiState = libraries)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

  private val _navState = MutableStateFlow(NavUiState())
  val navState = _navState.stateIn(viewModelScope, SharingStarted.Lazily, NavUiState())

  init {
    viewModelScope.launch { _uiState.update { repository.remoteSync(it, fromLogin) } }

    viewModelScope.launch {
      progressRepo.finishedEpisodeIdsGroupedByLibraryItemId().collect {
        progress: Map<String, List<String>> ->
        _uiState.update { state ->
          if (_uiState.value.librariesUiState.isEmpty()) return@collect

          val updatedLibraries =
            state.librariesUiState.map { libraryUiState ->
              val updatedPodcasts =
                libraryUiState.podcasts.map { podcast ->
                  val finishedIds = progress[podcast.id] ?: emptyList()
                  if (finishedIds.isEmpty()) return@map podcast
                  val downloadedIds =
                    downloadRepo.fetchPodcast(podcast.id).map { it.request.id.substringAfter("|") }

                  val finishedEpisodesCount = finishedIds.count()
                  val unfinishedEpisodeCount = podcast.episodeCount - finishedEpisodesCount
                  val downloadedAndFinishedCount = finishedIds.count { it in downloadedIds }
                  val unfinishedAndDownloadCount =
                    podcast.downloadedCount - downloadedAndFinishedCount
                  podcast.copy(
                    unfinishedCount = unfinishedEpisodeCount,
                    unfinishedAndDownloadCount = unfinishedAndDownloadCount,
                  )
                }
              libraryUiState.copy(podcasts = updatedPodcasts)
            }
          state.copy(librariesUiState = updatedLibraries)
        }
      }
    }

    viewModelScope.launch {
      downloadRepo.completedDownloads.collect { downloads: List<Download> ->
        if (_uiState.value.librariesUiState.isEmpty()) return@collect

        _uiState.update { state ->
          val updatedLibraries =
            state.librariesUiState.map { libraryUiState ->
              if (libraryUiState.isBookLibrary) {
                updateLibraryWithBooks(libraryUiState)
              } else {
                updateLibraryWithPodcasts(libraryUiState, downloads)
              }
            }
          state.copy(librariesUiState = updatedLibraries)
        }
      }
    }
  }

  private fun updateLibraryWithBooks(libraryUiState: LibraryUiState): LibraryUiState {
    val updatedBooks =
      libraryUiState.books.map { book ->
        book.copy(isDownloaded = downloadRepo.isBookDownloaded(book.id, book.trackIndexes))
      }
    return libraryUiState.copy(books = updatedBooks)
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun updateLibraryWithPodcasts(
    libraryUiState: LibraryUiState,
    downloads: List<Download>,
  ): LibraryUiState {
    val updatedPodcasts =
      libraryUiState.podcasts.map { podcast -> updateCounts(podcast, downloads) }
    return libraryUiState.copy(podcasts = updatedPodcasts)
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun updateCounts(podcast: PodcastUiState, downloads: List<Download>): PodcastUiState {
    val finishedIds = progressRepo.finishedEpisodeIdsByLibraryItemId(podcast.id)
    val downloadedIds =
      downloads
        .filter { it.request.id.substringBefore("|") == podcast.id }
        .map { it.request.id.substringAfter("|") }

    val downloadedCount = downloadedIds.size
    val downloadedAndFinishedCount = finishedIds.count { it in downloadedIds }
    val unfinishedAndDownloadCount = downloadedCount - downloadedAndFinishedCount
    return podcast.copy(
      downloadedCount = downloadedCount,
      unfinishedAndDownloadCount = unfinishedAndDownloadCount,
    )
  }

  fun onEvent(event: HomeEvent) {
    when (event) {
      is HomeEvent.RefreshLibrary -> {
        _uiState.update { it.copy(homeState = HomeState.Loading, currentPage = event.page) }
        viewModelScope.launch { _uiState.update { repository.remoteSync(it) } }
      }
      is HomeEvent.ChangeLibrary -> {
        _uiState.update { it.copy(currentPage = event.page) }
      }
      is HomeEvent.Navigate -> {
        viewModelScope.launch {
          _navState.update {
            _navState.value.copy(id = event.id, isBook = event.isBook, isNavigate = true)
          }
        }
      }
      is HomeEvent.HomeDisplayPrefsEvent -> {
        when (event.displayPrefsEvent) {
          is DisplayPrefsEvent.BookSort -> {
            _uiState.update { state ->
              val bookSort = BookSort.fromLabel(event.displayPrefsEvent.bookSort)
              viewModelScope.launch { settingsRepository.updateBookSort(bookSort) }
              val updatedDisplayPrefs = state.prefs.displayPrefs.copy(bookSort = bookSort)
              val prefs = state.prefs.copy(displayPrefs = updatedDisplayPrefs)
              state.copy(prefs = prefs)
            }
          }
          is DisplayPrefsEvent.Filter -> {
            _uiState.update { state ->
              val filter = Filter.valueOf(event.displayPrefsEvent.filter)
              viewModelScope.launch { settingsRepository.updateFilter(filter) }
              val updatedDisplayPrefs = state.prefs.displayPrefs.copy(filter = filter)
              val prefs = state.prefs.copy(displayPrefs = updatedDisplayPrefs)
              state.copy(prefs = prefs)
            }
          }
          is DisplayPrefsEvent.PodcastSort -> {
            _uiState.update { state ->
              val podcastSort = PodcastSort.fromLabel(event.displayPrefsEvent.podcastSort)
              viewModelScope.launch { settingsRepository.updatePodcastSort(podcastSort) }
              val updatedDisplayPrefs = state.prefs.displayPrefs.copy(podcastSort = podcastSort)
              val prefs = state.prefs.copy(displayPrefs = updatedDisplayPrefs)
              state.copy(prefs = prefs)
            }
          }
          is DisplayPrefsEvent.PodcastSortOrder -> {
            _uiState.update { state ->
              val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
              viewModelScope.launch { settingsRepository.updatePodcastSortOrder(sortOrder) }
              val updatedDisplayPrefs = state.prefs.displayPrefs.copy(podcastSortOrder = sortOrder)
              val prefs = state.prefs.copy(displayPrefs = updatedDisplayPrefs)
              state.copy(prefs = prefs)
            }
          }
          is DisplayPrefsEvent.SortOrder -> {
            _uiState.update { state ->
              val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
              viewModelScope.launch { settingsRepository.updateSortOrder(sortOrder) }
              val updatedDisplayPrefs = state.prefs.displayPrefs.copy(sortOrder = sortOrder)
              val prefs = state.prefs.copy(displayPrefs = updatedDisplayPrefs)
              state.copy(prefs = prefs)
            }
          }
        }
      }
    }
  }

  fun resetNavigationState() {
    _navState.update { it.copy(isNavigate = false) }
  }
}

data class NavUiState(
  val id: String = "",
  val isBook: Boolean = true,
  val isNavigate: Boolean = false,
)

sealed class HomeEvent {
  data class ChangeLibrary(val page: Int) : HomeEvent()

  data class RefreshLibrary(val page: Int) : HomeEvent()

  data class Navigate(val id: String, val isBook: Boolean) : HomeEvent()

  data class HomeDisplayPrefsEvent(val displayPrefsEvent: DisplayPrefsEvent) : HomeEvent()
}
