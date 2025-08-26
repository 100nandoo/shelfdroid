package dev.halim.shelfdroid.core.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val repository: HomeRepository,
  private val progressRepo: ProgressRepo,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())

  val uiState: StateFlow<HomeUiState> =
    combine(_uiState, settingsRepository.listView, progressRepo.flowFinishedEpisodesCountById()) {
        uiState: HomeUiState,
        listView: Boolean,
        progress: List<Pair<String, Long>> ->
        if (uiState.librariesUiState.isNotEmpty()) {
          val currentLibrary = uiState.librariesUiState[uiState.currentPage]
          val updatedPodcasts =
            currentLibrary.podcasts.map { podcast ->
              val update = progress.find { it.first == podcast.id }?.second?.toInt()
              if (update != null) {
                val unfinishedEpisodeCount = podcast.episodeCount - update
                podcast.copy(unfinishedEpisodeCount = unfinishedEpisodeCount)
              } else podcast
            }
          val updatedLibraries = uiState.librariesUiState.toMutableList()
          updatedLibraries[uiState.currentPage] =
            uiState.librariesUiState[uiState.currentPage].copy(podcasts = updatedPodcasts)
          uiState.copy(listView = listView, librariesUiState = updatedLibraries)
        } else {
          uiState.copy(listView = listView)
        }
      }
      .onStart { onStartApis() }
      .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

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
        prefetchLibraryItems(event.page)
      }
      is HomeEvent.Navigate -> {
        viewModelScope.launch {
          _navState.update {
            _navState.value.copy(id = event.id, isBook = event.isBook, isNavigate = true)
          }
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
    viewModelScope.launch(handler) {
      val libraries = repository.getLibraries()
      _uiState.update { it.copy(homeState = HomeState.Success, librariesUiState = libraries) }
      fetchUser()
      prefetchLibraryItems(0)
    }
  }

  private fun prefetchLibraryItems(page: Int) {
    val currentState = _uiState.value
    val libraries = currentState.librariesUiState

    // Prefetch current page if not loaded
    if (page < libraries.size) {
      fetchLibraryItems(page)
    }

    // Prefetch next page if not loaded
    val nextPage = page + 1
    if (nextPage < libraries.size) {
      fetchLibraryItems(nextPage)
    }
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
    viewModelScope.launch { _uiState.update { repository.getUser(_uiState.value) } }
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
}
