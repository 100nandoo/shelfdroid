package dev.halim.shelfdroid.core.ui.screen.podcastfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.response.PodcastFolder
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedRepository
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedUiState
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastFeedViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: PodcastFeedRepository) :
  ViewModel() {
  val payload: PodcastFeedNavPayload = savedStateHandle.toRoute()
  private val _uiState = MutableStateFlow(PodcastFeedUiState())

  val uiState: StateFlow<PodcastFeedUiState> = _uiState

  init {
    viewModelScope.launch { _uiState.update { repository.feed(payload.feedUrl, payload) } }
  }

  fun onEvent(event: PodcastFeedEvent) {
    when (event) {
      is PodcastFeedEvent.SubmitButtonPressed -> {
        viewModelScope.launch {
          _uiState.update { repository.createPodcast(payload, _uiState.value) }
        }
      }
      is PodcastFeedEvent.TitleChanged -> _uiState.update { it.copy(title = event.text) }
      is PodcastFeedEvent.AuthorChanged -> _uiState.update { it.copy(author = event.text) }
      is PodcastFeedEvent.FeedUrlChanged -> _uiState.update { it.copy(feedUrl = event.text) }
      is PodcastFeedEvent.DescriptionChanged ->
        _uiState.update { it.copy(description = event.text) }
      is PodcastFeedEvent.LanguageChanged -> _uiState.update { it.copy(language = event.text) }
      is PodcastFeedEvent.PathChanged -> _uiState.update { it.copy(path = event.text) }
      is PodcastFeedEvent.AutoDownloadChanged ->
        _uiState.update { it.copy(autoDownload = event.enabled) }
      is PodcastFeedEvent.FolderSelected ->
        _uiState.update { it.copy(selectedFolder = event.folder) }

      is PodcastFeedEvent.GenreAdded ->
        _uiState.update { it.copy(genres = (it.genres + event.text).distinct().sorted()) }
      is PodcastFeedEvent.GenreRemoved ->
        _uiState.update { it.copy(genres = it.genres - event.text) }
    }
  }
}

sealed class PodcastFeedEvent {
  data object SubmitButtonPressed : PodcastFeedEvent()

  data class TitleChanged(val text: String) : PodcastFeedEvent()

  data class AuthorChanged(val text: String) : PodcastFeedEvent()

  data class FeedUrlChanged(val text: String) : PodcastFeedEvent()

  data class DescriptionChanged(val text: String) : PodcastFeedEvent()

  data class PathChanged(val text: String) : PodcastFeedEvent()

  data class AutoDownloadChanged(val enabled: Boolean) : PodcastFeedEvent()

  data class FolderSelected(val folder: PodcastFolder) : PodcastFeedEvent()

  data class GenreAdded(val text: String) : PodcastFeedEvent()

  data class GenreRemoved(val text: String) : PodcastFeedEvent()

  data class LanguageChanged(val text: String) : PodcastFeedEvent()
}
