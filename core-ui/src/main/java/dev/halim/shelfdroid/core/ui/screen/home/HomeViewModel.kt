package dev.halim.shelfdroid.core.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.home.HomeRepository
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
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
class HomeViewModel @Inject constructor(private val homeRepository: HomeRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> =
    _uiState.onStart { onStartApis() }.stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

  private val _navState = MutableStateFlow(NavUiState())
  val navState = _navState.stateIn(viewModelScope, SharingStarted.Lazily, NavUiState())

  fun onEvent(event: HomeEvent) {
    when (event) {
      is HomeEvent.RefreshLibrary -> {
        fetchLibraryItems(event.page)
        fetchUser()
      }
      is HomeEvent.ChangeLibrary -> prefetchLibraryItems(event.page)
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
      val libraries = homeRepository.getLibraries()
      _uiState.update { it.copy(homeState = HomeState.Success, librariesUiState = libraries) }
      prefetchLibraryItems(0)
      fetchUser()
    }
  }

  private fun prefetchLibraryItems(page: Int) {
    val currentState = _uiState.value
    val libraries = currentState.librariesUiState
    val loadedItems = currentState.libraryItemsUiState

    // Prefetch current page if not loaded
    if (page < libraries.size && !loadedItems.containsKey(page)) {
      fetchLibraryItems(page)
    }

    // Prefetch next page if not loaded
    val nextPage = page + 1
    if (nextPage < libraries.size && !loadedItems.containsKey(nextPage)) {
      fetchLibraryItems(nextPage)
    }
  }

  private fun fetchLibraryItems(page: Int) {
    _uiState.update { it.copy(homeState = HomeState.Loading) }
    val id = _uiState.value.librariesUiState[page].id
    viewModelScope.launch(handler) {
      val libraryItems = homeRepository.getLibraryItems(id)
      _uiState.update { currentState ->
        val updatedMap = currentState.libraryItemsUiState.toMutableMap()
        updatedMap[page] = libraryItems
        currentState.copy(homeState = HomeState.Success, libraryItemsUiState = updatedMap)
      }
    }
  }

  private fun fetchUser() {
    _uiState.update { it.copy(homeState = HomeState.Loading) }
    viewModelScope.launch { _uiState.update { homeRepository.getUser(_uiState.value) } }
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
