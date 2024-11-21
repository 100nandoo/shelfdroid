package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.db.LibraryEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LibraryItem
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.network.User
import dev.halim.shelfdroid.network.libraryitem.Book
import dev.halim.shelfdroid.network.libraryitem.Podcast
import dev.halim.shelfdroid.store.LibraryStore
import dev.halim.shelfdroid.store.StoreData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.store.store5.impl.extensions.get
import kotlin.math.roundToLong

class HomeViewModel(
    private val api: Api,
    private val libraryStore: LibraryStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { apis() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HomeUiState())

    private val _navState = MutableStateFlow(Pair(false, BookUiState()))
    val navState = _navState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Pair(false, BookUiState()))

    private val libraryIds = mutableListOf<String>()
    private val libraryItemIds = mutableMapOf<String, List<String>>()

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.RefreshLibrary -> apis(homeEvent.page)
            is HomeEvent.ChangeLibrary -> apis(homeEvent.page)
            is HomeEvent.NavigateToPlayer -> {
                navigateToPlayer(homeEvent.bookUiState)
            }
        }
    }

    private fun navigateToPlayer(bookUiState: BookUiState) {
        viewModelScope.launch {
            _navState.update { _navState.value.copy(true, bookUiState) }
        }
    }

    fun resetNavigationState() {
        _navState.update { it.copy(first = false) }
    }

    private fun apis(page: Int = 0) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        viewModelScope.launch {
            val libraries = getLibraries()
            val librariesUiState = libraries.map { library ->
                LibraryUiState(library.id, library.name)
            }
            val ids = getLibraryItemIds(page)
            val libraryItems = getLibraryItems(ids)
            val user = getMe()
            val mappedList = mapToLibraryItemUiState(user, libraryItems)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    librariesUiState = librariesUiState,
                    libraryItemsUiState = mapOf(page to mappedList)
                )
            }
        }
    }

    private fun getCurrentPage(page: Int) {
        viewModelScope.launch {
            val ids = getLibraryItemIds(page)
            val libraryItems = getLibraryItems(ids)
            val user = getMe()
            val mappedList = mapToLibraryItemUiState(user, libraryItems)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    libraryItemsUiState = mapOf(page to mappedList)
                )
            }
        }
    }

    private suspend fun getLibraries(): List<LibraryEntity> {
        val libraryItems = mutableListOf<LibraryEntity>()
        val result = libraryStore.get("")
        if (result is StoreData.Collection) {
            val librariesResponse = result.data
            libraryItems.addAll(librariesResponse)
            if (libraryIds.isEmpty()) {
                libraryIds.addAll(libraryItems.map { it.id })
            }
        }
        return libraryItems
    }

    private suspend fun getLibraryItemIds(page: Int): List<String> {
        val libraryId = kotlin.runCatching { libraryIds[page] }.getOrNull()
        val ids = mutableListOf<String>()
        libraryId?.let {
            val result = api.libraryItems(libraryId)
            result.onSuccess { response ->
                ids.addAll(response.results.map { it.id })
                libraryItemIds[libraryId] = ids
            }
            result.onFailure { error ->
                _uiState.update { it.copy(homeState = HomeState.Failure(error.message)) }
            }
        }
        return ids
    }

    private suspend fun getLibraryItems(ids: List<String>): List<LibraryItem> {
        val list = mutableListOf<LibraryItem>()
        val result = api.batchLibraryItems(ids)
        result.onSuccess { response ->
            list.addAll(response.libraryItems)
        }
        result.onFailure { error ->
            _uiState.update { it.copy(homeState = HomeState.Failure(error.message)) }
        }
        return list
    }

    private suspend fun getMe(): User {
        var user = User()
        val result = api.me()
        result.onSuccess { response -> user = response }
        result.onFailure { error -> _uiState.update { it.copy(homeState = HomeState.Failure(error.message)) } }
        return user
    }

    private fun mapToLibraryItemUiState(
        response: User,
        list: List<LibraryItem>
    ): List<HomeLibraryItemUiState> {
        return list.map { libraryItem ->
            val mediaProgress =
                response.mediaProgress.firstOrNull { it.libraryItemId == libraryItem.id }
            val coverUrl = api.generateItemCoverUrl(libraryItem.id)
            when (libraryItem.mediaType) {
                "book" -> BookUiState.from(libraryItem, mediaProgress, coverUrl, api)
                else -> PodcastUiState(libraryItem, coverUrl)
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
    abstract val author: String
    abstract val title: String
    abstract val cover: String
}

@Serializable
data class BookUiState(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    val progress: Float = 0f,
    val url: String = "",
    val seekTime: Long = 0L
) : HomeLibraryItemUiState() {
    companion object {
        fun from(
            libraryItem: LibraryItem, mediaProgress: MediaProgress?,
            cover: String, api: Api
        ): BookUiState {
            val id = libraryItem.id
            val inoDurations = (libraryItem.media as Book).audioFiles
                .associate { it.ino to it.duration }
            val author = libraryItem.media.metadata.authors.joinToString { it.name }
            val title = libraryItem.media.metadata.title ?: ""
            val progress = mediaProgress?.progress ?: 0f
            val currentTime = mediaProgress?.currentTime ?: 0f
            val (url, seekTime) = findInoIdAndSeekTiming(id, inoDurations, currentTime, api)
            return BookUiState(
                id, author, title, cover, progress, url, seekTime
            )
        }

        private inline fun findInoIdAndSeekTiming(
            id: String,
            inoDurations: Map<String, Double>,
            currentTime: Float,
            api: Api
        ): Pair<String, Long> {
            var url = api.generateItemStreamUrl(id, inoDurations.keys.first())

            if (inoDurations.size == 1) {
                val seekTime = (currentTime * 1000).roundToLong()
                return url to seekTime
            }

            var cumulativeTime = 0.0
            for ((inoId, duration) in inoDurations) {
                cumulativeTime += duration

                if (currentTime <= cumulativeTime) {
                    url = api.generateItemStreamUrl(id, inoId)
                    val seekTime = (currentTime - (cumulativeTime - duration)).roundToLong()
                    return url to seekTime
                }
            }

            return url to 0L
        }
    }
}

data class PodcastUiState(
    override val id: String,
    override val author: String,
    override val title: String,
    override val cover: String,
    val episodeCount: Int
) : HomeLibraryItemUiState() {
    constructor(libraryItem: LibraryItem, cover: String) : this(
        libraryItem.id,
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
    data class ChangeLibrary(val page: Int) : HomeEvent()
    data class RefreshLibrary(val page: Int) : HomeEvent()
    data class NavigateToPlayer(val bookUiState: BookUiState) : HomeEvent()
}
