package dev.halim.shelfdroid.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.LibraryEntity
import dev.halim.shelfdroid.network.LibraryItem
import dev.halim.shelfdroid.network.libraryitem.Podcast
import dev.halim.shelfdroid.store.ItemExtensions.toUiState
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.ItemStore
import dev.halim.shelfdroid.store.LibraryKey
import dev.halim.shelfdroid.store.LibraryOutput
import dev.halim.shelfdroid.store.LibraryStore
import dev.halim.shelfdroid.store.StoreOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import org.mobilenativefoundation.store.store5.impl.extensions.get

class HomeViewModel(
    private val libraryStore: LibraryStore,
    private val itemStore: ItemStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { apis() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HomeUiState())

    private val _navState = MutableStateFlow(Pair(false, BookUiState()))
    val navState = _navState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Pair(false, BookUiState()))

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.RefreshLibrary -> apis(homeEvent.page)
            is HomeEvent.ChangeLibrary -> apis(homeEvent.page, false)
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

    private fun apis(page: Int = 0, fresh: Boolean = true) {
        _uiState.update { it.copy(homeState = HomeState.Loading) }
        viewModelScope.launch {
            val libraries = getLibraries(fresh)
            val librariesUiState = libraries.map { library ->
                LibraryUiState(library.id, library.name)
            }
            val libraryItems = getLibraryItems(libraries[page].id, fresh)
            _uiState.update {
                it.copy(
                    homeState = HomeState.Success,
                    librariesUiState = librariesUiState,
                    libraryItemsUiState = mapOf(page to libraryItems.map { it.toUiState() })
                )
            }
        }
    }

    private suspend fun getLibraries(fresh: Boolean): List<LibraryEntity> {
        val libraryItems = mutableListOf<LibraryEntity>()
        val result: LibraryOutput =
            if (fresh) libraryStore.fresh(LibraryKey.All as LibraryKey)
            else libraryStore.get(LibraryKey.All as LibraryKey)
        if (result is StoreOutput.Collection) {
            val librariesResponse = result.data
            libraryItems.addAll(librariesResponse)
        }
        return libraryItems
    }

    private suspend fun getLibraryItems(libraryId: String, fresh: Boolean): List<ItemEntity> {
        val list = mutableListOf<ItemEntity>()
        val itemIds = (libraryStore.get(LibraryKey.Single(libraryId)) as StoreOutput.Single).data.itemIds
        val result = if (fresh) itemStore.fresh(ItemKey.Collection(itemIds))
        else itemStore.get(ItemKey.Collection(itemIds))
        if (result is StoreOutput.Collection<ItemEntity>) {
            list.addAll(result.data)
        }
        return list
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
) : HomeLibraryItemUiState()

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
