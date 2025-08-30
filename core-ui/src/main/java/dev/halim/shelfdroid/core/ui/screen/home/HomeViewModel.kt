package dev.halim.shelfdroid.core.ui.screen.home

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@UnstableApi
@Inject
constructor(
  private val repository: HomeRepository,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())

  init {
    viewModelScope.launch {
      settingsRepository.displayPrefs.collect { displayPrefs ->
        _uiState.update { it.copy(displayPrefs = displayPrefs) }
      }
    }

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
              if (libraryUiState.isBook) {
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

  val uiState: StateFlow<HomeUiState> =
    _uiState.onStart { onStartApis() }.stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

  private val _navState = MutableStateFlow(NavUiState())
  val navState = _navState.stateIn(viewModelScope, SharingStarted.Lazily, NavUiState())

  fun onEvent(event: HomeEvent) {
    when (event) {
      is HomeEvent.RefreshLibrary -> {
        _uiState.update { it.copy(currentPage = event.page) }
        fetchLibraryItems(event.page)
        fetchUser()
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
      is HomeEvent.DownloadFilter -> {
        _uiState.update { state ->
          val filter = state.displayPrefs.filter.toggleDownloaded()
          viewModelScope.launch { settingsRepository.updateFilter(filter) }
          state.copy(displayPrefs = state.displayPrefs.copy(filter = filter))
        }
      }
    }
  }

  fun resetNavigationState() {
    _navState.update { it.copy(isNavigate = false) }
  }

  private val handler = CoroutineExceptionHandler { _, exception ->
    _uiState.update { it.copy(homeState = HomeState.Failure(exception.message)) }
  }

  private fun onStartApis() {
    _uiState.update { it.copy(homeState = HomeState.Loading) }
    viewModelScope.launch {
      val libraries = repository.getLibraries()
      _uiState.update { it.copy(homeState = HomeState.Success, librariesUiState = libraries) }
      fetchUser()
      prefetchLibraryItems()
    }
  }

  private fun prefetchLibraryItems() {
    val currentState = _uiState.value
    val libraries = currentState.librariesUiState

    libraries.forEachIndexed { index, _ -> fetchLibraryItems(index) }
  }

  private fun fetchLibraryItems(page: Int) {
    _uiState.update { it.copy(homeState = HomeState.Loading) }
    val library = _uiState.value.librariesUiState[page]
    viewModelScope.launch(handler) {
      val newLibrary = repository.getLibraryItems(library)
      _uiState.update { currentState ->
        val libraries = currentState.librariesUiState.toMutableList()
        libraries[page] = newLibrary
        currentState.copy(homeState = HomeState.Success, librariesUiState = libraries)
      }
    }
  }

  private fun fetchUser() {
    _uiState.update { it.copy(homeState = HomeState.Loading) }
    viewModelScope.launch {
      val result = repository.getUser(_uiState.value)
      _uiState.update { result }
    }
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

  data object DownloadFilter : HomeEvent()
}
