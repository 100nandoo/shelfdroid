package dev.halim.shelfdroid.core.ui.screen.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeRepository
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.core.ui.navigation.Episode
import dev.halim.shelfdroid.download.DownloadRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EpisodeViewModel.Factory::class)
class EpisodeViewModel
@AssistedInject
constructor(
  private val repository: EpisodeRepository,
  private val downloadRepo: DownloadRepo,
  @Assisted navKey: Episode,
) : ViewModel() {

  val itemId: String = navKey.itemId
  val episodeId: String = navKey.episodeId

  private val _uiState = MutableStateFlow(EpisodeUiState())
  val uiState: StateFlow<EpisodeUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EpisodeUiState())

  fun onEvent(event: EpisodeEvent) {
    val download = _uiState.value.download
    when (event) {
      is EpisodeEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            viewModelScope.launch { downloadRepo.downloadPodcastEpisode(download) }
          }
          is CommonDownloadEvent.DeleteDownload -> {
            viewModelScope.launch { downloadRepo.deletePodcastEpisode(download) }
          }
        }
      }
    }
  }

  private fun initUiState() {
    viewModelScope.launch {
      repository.item(itemId, episodeId).collect { episodeUiState ->
        _uiState.value = episodeUiState
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Episode): EpisodeViewModel
  }
}

sealed interface EpisodeEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : EpisodeEvent
}
