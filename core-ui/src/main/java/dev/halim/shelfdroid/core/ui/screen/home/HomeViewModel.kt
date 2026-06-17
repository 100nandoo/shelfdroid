package dev.halim.shelfdroid.core.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.core.ui.navigation.Home
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = HomeViewModel.Factory::class)
class HomeViewModel
@UnstableApi
@AssistedInject
constructor(
  @Assisted private val navKey: Home,
  private val repository: HomeRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> =
    combine(_uiState, repository.item()) { state, (prefs, libraries) ->
        state.copy(prefs = prefs, librariesUiState = libraries)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

  init {
    viewModelScope.launch { _uiState.update { repository.remoteSync(it, navKey.fromLogin) } }
  }

  fun onEvent(event: HomeEvent) {
    when (event) {
      is HomeEvent.RefreshLibrary -> {
        _uiState.update { it.copy(state = GenericState.Loading, currentPage = event.page) }
        viewModelScope.launch { _uiState.update { repository.remoteSync(it) } }
      }
      is HomeEvent.ChangeLibrary -> {
        _uiState.update { it.copy(currentPage = event.page) }
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
      is HomeEvent.Delete -> {
        viewModelScope.launch {
          _uiState.update {
            repository.deleteItem(it, event.libraryId, event.itemId, event.isBook, event.hardDelete)
          }
        }
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Home): HomeViewModel
  }
}

sealed interface HomeEvent {
  data class ChangeLibrary(val page: Int) : HomeEvent

  data class RefreshLibrary(val page: Int) : HomeEvent

  data class HomeDisplayPrefsEvent(val displayPrefsEvent: DisplayPrefsEvent) : HomeEvent

  data class Delete(
    val libraryId: String,
    val itemId: String,
    val isBook: Boolean,
    val hardDelete: Boolean,
  ) : HomeEvent
}
