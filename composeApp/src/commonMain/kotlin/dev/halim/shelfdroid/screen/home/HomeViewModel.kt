package dev.halim.shelfdroid.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LibrariesResponse
import dev.halim.shelfdroid.network.BatchLibraryItemsRequest
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

            HomeEvent.ChangeLibrary -> {

            }

            HomeEvent.RefreshLibrary -> {
                libraries()
            }
        }
    }

    fun updateUiState(newState: HomeUiState) {
        _uiState.value = newState
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
                    val libraryIds = librariesResponse?.libraries?.map { it.id } ?: listOf()
                    fetchLibraryItems(libraryIds)
                },
                errorStateUpdater = { errorMessage ->
                    _uiState.value = _uiState.value.copy(homeState = HomeState.Failure(errorMessage))
                },
                apiCall = { api.libraries() }
            )
        }
    }

    private suspend fun fetchLibraryItems(libraryIds: List<String>) {
        api.handleApiCall(
            successStateUpdater = { libraryItemsResponse ->
                val response = libraryItemsResponse
                _uiState.value = _uiState.value.copy(
                    homeState = HomeState.Success,
                )
            },
            errorStateUpdater = { errorMessage ->
                _uiState.value = _uiState.value.copy(homeState = HomeState.Failure(errorMessage))
            },
            apiCall = { api.batchLibraryItems(BatchLibraryItemsRequest(libraryIds)) }
        )
    }

}

data class HomeUiState(
    val homeState: HomeState = HomeState.NotLoggedOut,
    val librariesResponse: LibrariesResponse = LibrariesResponse()
)

sealed class HomeState {
    data object NotLoggedOut : HomeState()
    data object Loading : HomeState()
    data object Success : HomeState()
    data class Failure(val errorMessage: String) : HomeState()
}

sealed class HomeEvent {
    data object LibraryItemPressed : HomeEvent()
    data object ChangeLibrary : HomeEvent()
    data object RefreshLibrary : HomeEvent()
}
