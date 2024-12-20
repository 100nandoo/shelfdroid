package dev.halim.shelfdroid.ui.screens.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.repo.PodcastRepository
import dev.halim.shelfdroid.ui.MediaItemPodcast
import dev.halim.shelfdroid.ui.MediaItemType
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PodcastViewModel(
    private val podcastRepository: PodcastRepository, private val mediaManager: MediaManager,
    private val id: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PodcastUiState())

    val playerState = mediaManager.playerState
        .stateIn(viewModelScope, SharingStarted.Lazily, MediaPlayerState())

    private fun initUiState() {
        viewModelScope.launch {
            _uiState.update { podcastRepository.getPodcastModel(id) }
        }
    }

    fun onEvent(event: PodcastEvent) {
        when (event) {
            is PodcastEvent.Play -> mediaManager.playPodcast(event.episodeUiState.toMediaItemPodcast())
            is PodcastEvent.Navigate -> TODO()
        }
    }
}

sealed class PodcastState {
    data object Loading : PodcastState()
    data object Success : PodcastState()
    data class Failure(val errorMessage: String?) : PodcastState()
}

data class PodcastUiState(
    val state: PodcastState = PodcastState.Loading,
    val author: String = "",
    val title: String = "",
    val cover: String = "",
    val description: String = "",
    val episodes: List<EpisodeUiState> = emptyList()
)

@Serializable
data class EpisodeUiState(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    override val url: String = "",
    override val seekTime: Long = 0L,
    override val type: MediaItemType = MediaItemType.Podcast,
    val libraryItemId: String = "",
    val description: String = "",
    val publishedAt: Long = 0L,
    val progress: Float = 0f,
) : ShelfdroidMediaItem() {
    fun toMediaItemPodcast(): MediaItemPodcast {
        return MediaItemPodcast(
            id = id,
            author = author,
            title = title,
            cover = cover,
            url = url,
            seekTime = seekTime,
            type = type,
            libraryItemId = libraryItemId,
        )
    }
}

sealed class PodcastEvent {
    data class Play(val episodeUiState: EpisodeUiState) : PodcastEvent()
    data class Navigate(val id: String) : PodcastEvent()
}