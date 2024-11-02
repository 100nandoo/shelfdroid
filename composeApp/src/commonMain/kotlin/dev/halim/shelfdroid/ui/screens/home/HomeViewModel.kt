package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LibrariesResponse
import dev.halim.shelfdroid.network.LibraryItemsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val api: Api,
) : ViewModel() {


    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        libraries()
    }

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.LibraryItemPressed -> viewModelScope.launch {

            }
            HomeEvent.RefreshLibrary -> {
                libraries()
            }
            is HomeEvent.ChangeLibrary -> {
                val page = homeEvent.page
                fetchLibraryItems(_uiState.value.librariesResponse.libraries[page].id)
            }
        }
    }

    private fun libraries() {
        _uiState.value = _uiState.value.copy(homeState = HomeState.Loading)
        viewModelScope.launch {
            api.handleApiCall(
                successStateUpdater = { librariesResponse ->
                    _uiState.value = _uiState.value.copy(
                        homeState = HomeState.Success,
                        librariesResponse = librariesResponse ?: LibrariesResponse()
                    )
                },
                errorStateUpdater = { errorMessage ->
                    _uiState.value =
                        _uiState.value.copy(homeState = HomeState.Failure(errorMessage))
                },
                apiCall = { api.libraries() }
            )
        }
    }

    private fun fetchLibraryItems(libraryId: String) {
        viewModelScope.launch {
            api.handleApiCall(
                successStateUpdater = { libraryItemsResponse ->
                    libraryItemsResponse?.let {
                        _uiState.value = _uiState.value.copy(
                            homeState = HomeState.Success, libraryItemsResponse = libraryItemsResponse
                        )
                    }

                },
                errorStateUpdater = { errorMessage ->
                    _uiState.value = _uiState.value.copy(homeState = HomeState.Failure(errorMessage))
                },
                apiCall = { api.libraryItems(libraryId) }
            )
        }
    }
}

data class HomeUiState(
    val homeState: HomeState = HomeState.NotLoggedOut,
    val librariesResponse: LibrariesResponse = LibrariesResponse(),
    val libraryItemsResponse: LibraryItemsResponse = LibraryItemsResponse()
)

sealed class HomeState {
    data object NotLoggedOut : HomeState()
    data object Loading : HomeState()
    data object Success : HomeState()
    data class Failure(val errorMessage: String) : HomeState()
}

sealed class HomeEvent {
    data object LibraryItemPressed : HomeEvent()
    data class ChangeLibrary(val page: Int) : HomeEvent()
    data object RefreshLibrary : HomeEvent()
}
