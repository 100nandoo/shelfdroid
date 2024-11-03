package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.Library
import dev.halim.shelfdroid.network.LibraryItem
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.network.User
import dev.halim.shelfdroid.network.libraryitem.Book
import dev.halim.shelfdroid.network.libraryitem.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val api: Api,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {


    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val libraryIds = mutableListOf<String>()
    private val libraryItemIds = mutableMapOf<String, List<String>>()

    init {
        apis()
    }

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.LibraryItemPressed -> Unit
            is HomeEvent.RefreshLibrary -> apis(homeEvent.page)
            is HomeEvent.ChangeLibrary -> apis(homeEvent.page)
        }
    }

    private fun apis(page: Int = 0) {
        _uiState.value = _uiState.value.copy(homeState = HomeState.Loading)
        viewModelScope.launch {
            val libraries = getLibraries()
            val librariesUiState = libraries.map { library ->
                LibraryUiState(library.id, library.name)
            }
            val ids = getLibraryItemIds(page)
            val libraryItems = getLibraryItems(ids)
            val user = getMe()
            val mappedList = mapToLibraryItemUiState(user, libraryItems)
            _uiState.value = _uiState.value.copy(
                homeState = HomeState.Success,
                librariesUiState = librariesUiState,
                libraryItemsUiState = mapOf(page to mappedList)
            )
        }
    }

    private fun getCurrentPage(page: Int) {
        viewModelScope.launch {
            val ids = getLibraryItemIds(page)
            val libraryItems = getLibraryItems(ids)
            val user = getMe()
            val mappedList = mapToLibraryItemUiState(user, libraryItems)
            _uiState.value = _uiState.value.copy(
                homeState = HomeState.Success,
                libraryItemsUiState = mapOf(page to mappedList)
            )
        }
    }

    private suspend fun getLibraries(): List<Library> {
        val libraryItems = mutableListOf<Library>()
        api.libraries().collect { result ->
            result.onSuccess { librariesResponse ->
                libraryItems.addAll(librariesResponse.libraries)
                if (libraryIds.isEmpty()) {
                    libraryIds.addAll(libraryItems.map { it.id })
                }
            }
            result.onFailure { error ->
                _uiState.value =
                    _uiState.value.copy(homeState = HomeState.Failure(error.message))
            }
        }
        return libraryItems
    }

    private suspend fun getLibraryItemIds(page: Int): List<String> {
        val libraryId = kotlin.runCatching { libraryIds[page] }.getOrNull()
        val ids = mutableListOf<String>()
        libraryId?.let {
            api.libraryItems(libraryId).collect { result ->
                result.onSuccess { response ->
                    ids.addAll(response.results.map { it.id })
                    libraryItemIds[libraryId] = ids
                }
                result.onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(homeState = HomeState.Failure(error.message))
                }
            }
        }
        return ids
    }

    private suspend fun getLibraryItems(ids: List<String>): List<LibraryItem> {
        val list = mutableListOf<LibraryItem>()
        api.batchLibraryItems(ids).collect { result ->
            result.onSuccess { response ->
                list.addAll(response.libraryItems)
            }
            result.onFailure { error ->
                _uiState.value =
                    _uiState.value.copy(homeState = HomeState.Failure(error.message))
            }
        }
        return list
    }

    private suspend fun getMe(): User {
        var user = User()
        api.me().collect { result ->
            result.onSuccess { response ->
                user = response
            }
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(homeState = HomeState.Failure(error.message))
            }
        }
        return user
    }

    private fun mapToLibraryItemUiState(
        response: User,
        list: List<LibraryItem>
    ): List<HomeLibraryItemUiState> {
        return list.map { libraryItem ->
            val mediaProgress =
                response.mediaProgress.firstOrNull { it.libraryItemId == libraryItem.id }
            val url = api.generateItemCoverUrl(libraryItem.id)
            when (libraryItem.mediaType) {
                "book" -> BookUiState(libraryItem, mediaProgress, url)
                else -> PodcastUiState(libraryItem, url)
            }
        }
    }
}

data class HomeUiState(
    val homeState: HomeState = HomeState.Loading,
    val librariesUiState: List<LibraryUiState> = emptyList(),
    val libraryItemsUiState: Map<Int, List<HomeLibraryItemUiState>> = emptyMap()
)

sealed class HomeLibraryItemUiState {
    abstract val id: String
    abstract val ino: String
    abstract val author: String
    abstract val title: String
    abstract val cover: String
}

data class BookUiState(
    override val id: String,
    override val ino: String,
    override val author: String,
    override val title: String,
    override val cover: String,
    val duration: Double,
    val progress: Double,
    val currentTime: Double,
) : HomeLibraryItemUiState() {
    constructor(libraryItem: LibraryItem, mediaProgress: MediaProgress?, cover: String) : this(
        libraryItem.id,
        libraryItem.ino,
        (libraryItem.media as Book).metadata.authors.joinToString { it.name },
        libraryItem.media.metadata.title ?: "",
        cover,
        mediaProgress?.duration ?: 0.0,
        mediaProgress?.progress ?: 0.0,
        mediaProgress?.currentTime ?: 0.0
    )
}

data class PodcastUiState(
    override val id: String,
    override val ino: String,
    override val author: String,
    override val title: String,
    override val cover: String,
    val episodeCount: Int
) : HomeLibraryItemUiState() {
    constructor(libraryItem: LibraryItem, cover: String) : this(
        libraryItem.id,
        libraryItem.ino,
        (libraryItem.media as Podcast).metadata.author ?: "",
        libraryItem.media.metadata.title ?: "",
        cover,
        libraryItem.media.episodes.size
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
    data object LibraryItemPressed : HomeEvent()
    data class ChangeLibrary(val page: Int) : HomeEvent()
    data class RefreshLibrary(val page: Int) : HomeEvent()
}
