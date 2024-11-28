package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.repo.HomeRepository
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val mediaManager: MediaManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { apis(fresh = true) }
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    private val _navState = MutableStateFlow(Pair(false, ""))
    val navState = _navState
        .stateIn(viewModelScope, SharingStarted.Lazily, Pair(false, ""))

    val playerState = mediaManager.playerState
        .stateIn(viewModelScope, SharingStarted.Lazily, MediaPlayerState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.RefreshLibrary -> currentLibraryItems(event.page, true)
            is HomeEvent.ChangeLibrary -> currentLibraryItems(event.page, false)
            is HomeEvent.NavigateToPlayer -> {
                navigateToPlayer(event.bookUiState.id)
            }

            is HomeEvent.PlayBook -> {
                mediaManager.playBookUiState(event.mediaItemImpl)
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

    private fun apis(page: Int = 0, fresh: Boolean = false) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        viewModelScope.launch(handler) {
            val libraries = homeRepository.getLibraries(fresh)
            val libraryItems = homeRepository.getLibraryItems(libraries[page].id, fresh)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    librariesUiState = libraries,
                    libraryItemsUiState = mapOf(page to libraryItems)
                )
            }
        }
    }

    private fun currentLibraryItems(page: Int, fresh: Boolean) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        val id = _uiState.value.librariesUiState[page].id
        viewModelScope.launch(handler) {
            val libraryItems = homeRepository.getLibraryItems(id, fresh)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    libraryItemsUiState = mapOf(page to libraryItems)
                )
            }
        }
    }
}

data class HomeUiState(
    val homeState: HomeState = HomeState.Loading,
    val librariesUiState: List<LibraryUiState> = emptyList(),
    val libraryItemsUiState: Map<Int, List<ShelfdroidMediaItem>> = emptyMap()
)

@Serializable
data class BookUiState(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    override val url: String = "",
    override val seekTime: Long = 0L,
    override val startTime: Long = 0L,
    override val endTime: Long = 0L,
    val progress: Float = 0f,
    val chapters: List<BookChapter> = emptyList()
) : ShelfdroidMediaItem() {
    override fun toImpl(): ShelfdroidMediaItemImpl = ShelfdroidMediaItemImpl(
        id, author, title, cover, url, seekTime,
        startTime, endTime
    )
}

data class PodcastUiState(
    override val id: String,
    override val author: String,
    override val title: String,
    override val cover: String,
    override val url: String,
    override val seekTime: Long = 0L,
    override val startTime: Long,
    override val endTime: Long,
    val episodeCount: Int,
) : ShelfdroidMediaItem() {
    override fun toImpl(): ShelfdroidMediaItemImpl = ShelfdroidMediaItemImpl(
        id, author, title, cover, url, seekTime,
        startTime, endTime
    )
}

data class LibraryUiState(
    val id: String = "",
    val name: String = "",
)

sealed class HomeState {
    data object Loading : HomeState()
    data object Success : HomeState()
    data class Failure(val errorMessage: String?) : HomeState()
}

sealed class HomeEvent {
    data class ChangeLibrary(val page: Int) : HomeEvent()
    data class RefreshLibrary(val page: Int) : HomeEvent()
    data class NavigateToPlayer(val bookUiState: BookUiState) : HomeEvent()
    data class PlayBook(val mediaItemImpl: ShelfdroidMediaItemImpl) : HomeEvent()
}
