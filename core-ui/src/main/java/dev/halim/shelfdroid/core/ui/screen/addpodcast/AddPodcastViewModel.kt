package dev.halim.shelfdroid.core.ui.screen.addpodcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.catalog.LibraryFolder
import dev.halim.shelfdroid.core.data.screen.addpodcast.AddPodcastRepository
import dev.halim.shelfdroid.core.data.screen.addpodcast.AddPodcastUiState
import dev.halim.shelfdroid.core.navigation.PodcastSourceFeedNavPayload
import dev.halim.shelfdroid.core.ui.navigation.AddPodcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AddPodcastViewModel.Factory::class)
class AddPodcastViewModel
@AssistedInject
constructor(
  @Assisted navKey: AddPodcast,
  private val repository: AddPodcastRepository,
) : ViewModel() {
  val payload: PodcastSourceFeedNavPayload = navKey.payload
  private val _uiState = MutableStateFlow(AddPodcastUiState())

  val uiState: StateFlow<AddPodcastUiState> = _uiState

  init {
    viewModelScope.launch {
      _uiState.update { repository.fetchPodcastSourceFeed(payload.feedUrl, payload) }
    }
  }

  fun onEvent(event: PodcastSourceFeedEvent) {
    when (event) {
      is PodcastSourceFeedEvent.SubmitButtonPressed -> {
        viewModelScope.launch {
          _uiState.update { repository.createPodcast(payload, _uiState.value) }
        }
      }
      is PodcastSourceFeedEvent.TitleChanged -> _uiState.update { it.copy(title = event.text) }
      is PodcastSourceFeedEvent.AuthorChanged -> _uiState.update { it.copy(author = event.text) }
      is PodcastSourceFeedEvent.FeedUrlChanged -> _uiState.update { it.copy(feedUrl = event.text) }
      is PodcastSourceFeedEvent.DescriptionChanged ->
        _uiState.update { it.copy(description = event.text) }
      is PodcastSourceFeedEvent.LanguageChanged -> _uiState.update { it.copy(language = event.text) }
      is PodcastSourceFeedEvent.PathChanged -> _uiState.update { it.copy(path = event.text) }
      is PodcastSourceFeedEvent.AutoDownloadChanged ->
        _uiState.update { it.copy(autoDownload = event.enabled) }
      is PodcastSourceFeedEvent.FolderSelected ->
        _uiState.update { it.copy(selectedFolder = event.folder) }

      is PodcastSourceFeedEvent.GenreAdded ->
        _uiState.update { it.copy(genres = (it.genres + event.text).distinct().sorted()) }
      is PodcastSourceFeedEvent.GenreRemoved ->
        _uiState.update { it.copy(genres = it.genres - event.text) }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: AddPodcast): AddPodcastViewModel
  }
}

sealed interface PodcastSourceFeedEvent {
  data object SubmitButtonPressed : PodcastSourceFeedEvent

  data class TitleChanged(val text: String) : PodcastSourceFeedEvent

  data class AuthorChanged(val text: String) : PodcastSourceFeedEvent

  data class FeedUrlChanged(val text: String) : PodcastSourceFeedEvent

  data class DescriptionChanged(val text: String) : PodcastSourceFeedEvent

  data class PathChanged(val text: String) : PodcastSourceFeedEvent

  data class AutoDownloadChanged(val enabled: Boolean) : PodcastSourceFeedEvent

  data class FolderSelected(val folder: LibraryFolder) : PodcastSourceFeedEvent

  data class GenreAdded(val text: String) : PodcastSourceFeedEvent

  data class GenreRemoved(val text: String) : PodcastSourceFeedEvent

  data class LanguageChanged(val text: String) : PodcastSourceFeedEvent
}
