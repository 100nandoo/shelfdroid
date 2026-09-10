package dev.halim.shelfdroid.core.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.reconcileActiveLibraryId
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminEventRepository
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.data.sync.SyncCoordinator
import dev.halim.shelfdroid.core.data.sync.SyncEvent
import dev.halim.shelfdroid.core.prefs.BookSort
import dev.halim.shelfdroid.core.prefs.Filter
import dev.halim.shelfdroid.core.prefs.PodcastSort
import dev.halim.shelfdroid.core.prefs.SortOrder
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.core.ui.navigation.Home
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = HomeViewModel.Factory::class)
class HomeViewModel
@UnstableApi
@AssistedInject
constructor(
  @Assisted private val navKey: Home,
  private val repository: HomeRepository,
  private val libraryDataRepository: LibraryDataRepository,
  @Suppress("UNUSED_PARAMETER")
  private val libraryAdminEventRepository: LibraryAdminEventRepository,
  private val syncCoordinator: SyncCoordinator,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      settingsRepository.prefs.collect { prefs ->
        _uiState.update { state -> state.copy(prefs = prefs) }
      }
    }
    viewModelScope.launch {
      repository.item().collect { libraries ->
        _uiState.update { state ->
          state.copy(
            activeLibraryId =
              reconcileActiveLibraryId(
                previousLibraries = state.librariesUiState,
                activeLibraryId = state.activeLibraryId,
                updatedLibraries = libraries,
              ),
            librariesUiState = libraries,
          )
        }
      }
    }
    refresh(if (navKey.fromLogin) SyncEvent.AfterLogin else SyncEvent.UserRequested)
  }

  fun onEvent(event: HomeEvent) {
    when (event) {
      is HomeEvent.RefreshLibrary -> {
        _uiState.update { it.copy(state = GenericState.Loading, currentPage = event.page) }
        refresh(SyncEvent.UserRequested)
      }
      is HomeEvent.ChangeLibrary -> {
        _uiState.update { state ->
          state.copy(
            currentPage = event.page,
            activeLibraryId =
              state.librariesUiState.getOrNull(event.page)?.id ?: state.activeLibraryId,
          )
        }
      }
      is HomeEvent.HomeDisplayPrefsEvent -> {
        when (event.displayPrefsEvent) {
          is DisplayPrefsEvent.BookSort -> {
            val bookSort = BookSort.fromLabel(event.displayPrefsEvent.bookSort)
            _uiState.update { state ->
              val displayPrefs = state.prefs.displayPrefs.selectBookSort(bookSort)
              state.copy(prefs = state.prefs.copy(displayPrefs = displayPrefs))
            }
            viewModelScope.launch {
              settingsRepository.updateBookSort(bookSort)
            }
          }
          is DisplayPrefsEvent.Filter -> {
            val filter = Filter.valueOf(event.displayPrefsEvent.filter)
            _uiState.update { state ->
              val displayPrefs = state.prefs.displayPrefs.copy(filter = filter)
              state.copy(prefs = state.prefs.copy(displayPrefs = displayPrefs))
            }
            viewModelScope.launch {
              settingsRepository.updateFilter(filter)
            }
          }
          is DisplayPrefsEvent.PodcastSort -> {
            val podcastSort = PodcastSort.fromLabel(event.displayPrefsEvent.podcastSort)
            _uiState.update { state ->
              val displayPrefs = state.prefs.displayPrefs.selectPodcastSort(podcastSort)
              state.copy(prefs = state.prefs.copy(displayPrefs = displayPrefs))
            }
            viewModelScope.launch {
              settingsRepository.updatePodcastSort(podcastSort)
            }
          }
          is DisplayPrefsEvent.PodcastSortOrder -> {
            val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
            _uiState.update { state ->
              val displayPrefs = state.prefs.displayPrefs.copy(podcastSortOrder = sortOrder)
              state.copy(prefs = state.prefs.copy(displayPrefs = displayPrefs))
            }
            viewModelScope.launch {
              settingsRepository.updatePodcastSortOrder(sortOrder)
            }
          }
          is DisplayPrefsEvent.SortOrder -> {
            val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
            _uiState.update { state ->
              val displayPrefs = state.prefs.displayPrefs.copy(sortOrder = sortOrder)
              state.copy(prefs = state.prefs.copy(displayPrefs = displayPrefs))
            }
            viewModelScope.launch {
              settingsRepository.updateSortOrder(sortOrder)
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

  private fun refresh(event: SyncEvent) {
    viewModelScope.launch {
      syncCoordinator.prepareSync(event)
      val result = libraryDataRepository.synchronize()
      syncCoordinator.syncBackgroundData()
      _uiState.update { state ->
        state.copy(
          state =
            if (result.isSuccess) {
              GenericState.Success
            } else {
              GenericState.Failure(result.error?.message)
            }
        )
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
