package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.LibraryEntity
import dev.halim.shelfdroid.db.ProgressEntity
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.store.ItemExtensions.toBookUiState
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.LibraryKey
import dev.halim.shelfdroid.store.LibraryOutput
import dev.halim.shelfdroid.store.ProgressKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asCollection
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class HomeViewModel(
    private val storeManager: StoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { apis(fresh = true) }
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    private val _navState = MutableStateFlow(Pair(false, ""))
    val navState = _navState
        .stateIn(viewModelScope, SharingStarted.Lazily, Pair(false, ""))

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.RefreshLibrary -> apis(homeEvent.page, true)
            is HomeEvent.ChangeLibrary -> apis(homeEvent.page)
            is HomeEvent.NavigateToPlayer -> {
                navigateToPlayer(homeEvent.bookUiState.id)
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
            val libraries = getLibraries(fresh)
            val librariesUiState = libraries.map { library ->
                LibraryUiState(library.id, library.name)
            }
            val libraryItems = getLibraryItems(libraries[page].id, fresh)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    librariesUiState = librariesUiState,
                    libraryItemsUiState = mapOf(page to libraryItems.map { it.toBookUiState() })
                )
            }
            launch {
                getMediaProgresses(fresh)
            }
        }

    }

    private suspend fun getLibraries(fresh: Boolean): List<LibraryEntity> {
        val libraryItems = mutableListOf<LibraryEntity>()
        val result: LibraryOutput =
            if (fresh) storeManager.libraryStore.freshOrCached(LibraryKey.All)
            else storeManager.libraryStore.cached(LibraryKey.All)
        libraryItems.addAll(result.asCollection().data)
        return libraryItems
    }

    private suspend fun getLibraryItems(libraryId: String, fresh: Boolean): List<ItemEntity> {
        val list = mutableListOf<ItemEntity>()
        val itemIds = storeManager.libraryStore.cached(LibraryKey.Single(libraryId)).asSingle().data.itemIds
        val itemKey = ItemKey.Collection(itemIds)
        val result =
            if (fresh) storeManager.itemStore.freshOrCached(itemKey)
            else storeManager.itemStore.cached(itemKey)
        list.addAll(result.asCollection().data)
        return list
    }

    private suspend fun getMediaProgresses(fresh: Boolean): List<ProgressEntity> {
        val result =
            if (fresh) storeManager.progressStore.freshOrCached(ProgressKey.All)
            else storeManager.progressStore.cached(ProgressKey.All)
        return result.asCollection().data
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
    val chapters: List<BookChapter> = emptyList(),
    val currentChapterId: Long = 0L,
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
}
