package dev.halim.shelfdroid.core.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.home.BookUiState
import dev.halim.shelfdroid.core.data.home.HomeRepository
import dev.halim.shelfdroid.core.data.home.HomeState
import dev.halim.shelfdroid.core.data.home.HomeUiState
import dev.halim.shelfdroid.core.data.home.ShelfdroidMediaItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val homeRepository: HomeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { apis() }
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    private val _navState = MutableStateFlow(Pair(false, ""))
    val navState = _navState
        .stateIn(viewModelScope, SharingStarted.Lazily, Pair(false, ""))


    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.RefreshLibrary -> currentLibraryItems(event.page)
            is HomeEvent.ChangeLibrary -> currentLibraryItems(event.page)
            is HomeEvent.NavigateToPlayer -> {
                navigateToPlayer(event.bookUiState.id)
            }

            is HomeEvent.PlayBook -> {
            }
        }
    }

    private fun navigateToPlayer(itemId: String) {
        viewModelScope.launch {
            _navState.update { _navState.value.copy(true, itemId) }
        }
    }

    fun resetNavigationState() {
        _navState.update { it.copy(first = false) }
    }

    private val handler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(homeState = HomeState.Failure(exception.message)) }
    }

    private fun apis(page: Int = 0) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        viewModelScope.launch(handler) {
            val libraries = homeRepository.getLibraries()
            val libraryItems = homeRepository.getLibraryItems(libraries[page].id)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    librariesUiState = libraries,
                    libraryItemsUiState = mapOf(page to libraryItems)
                )
            }
        }
    }

    private fun currentLibraryItems(page: Int) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        val id = _uiState.value.librariesUiState[page].id
        viewModelScope.launch(handler) {
            val libraryItems = homeRepository.getLibraryItems(id)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    libraryItemsUiState = mapOf(page to libraryItems)
                )
            }
        }
    }
}

sealed class HomeEvent {
    data class ChangeLibrary(val page: Int) : HomeEvent()
    data class RefreshLibrary(val page: Int) : HomeEvent()
    data class NavigateToPlayer(val bookUiState: BookUiState) : HomeEvent()
    data class PlayBook(val mediaItem: ShelfdroidMediaItem) : HomeEvent()
}
