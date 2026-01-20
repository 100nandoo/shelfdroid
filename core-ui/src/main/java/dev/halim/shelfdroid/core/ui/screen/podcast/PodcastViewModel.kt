package dev.halim.shelfdroid.core.ui.screen.podcast

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.media.service.StateHolder
import dev.halim.socketio.SocketManager
import dev.halim.socketio.SocketManager.Event.Episode as SocketEpisode
import dev.halim.socketio.model.PodcastEpisodeDownload
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

@HiltViewModel
class PodcastViewModel
@Inject
constructor(
  private val repository: PodcastRepository,
  private val downloadRepo: DownloadRepo,
  private val socketManager: SocketManager,
  private val json: Json,
  stateHolder: StateHolder,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))
  private val apiState = MutableStateFlow<PodcastApiState>(PodcastApiState.Idle)
  private val selectedEpisodeIds = MutableStateFlow<Set<String>>(emptySet())
  private val selectionMode = MutableStateFlow(false)

  init {
    startStopSocket(true)
  }

  override fun onCleared() {
    startStopSocket(false)
    super.onCleared()
  }

  val uiState: StateFlow<PodcastUiState> =
    combine(
        repository.item(id),
        stateHolder.uiState,
        apiState,
        selectionMode,
        selectedEpisodeIds,
      ) { podcast, playerState, apiState, selectionMode, selectedIds ->
        val updatedEpisodes =
          podcast.episodes.map { episode ->
            if (episode.episodeId == playerState.episodeId) {
              episode.copy(
                progress = playerState.playbackProgress.progress,
                isPlaying = playerState.exoState == ExoState.Playing,
              )
            } else episode
          }
        podcast.copy(
          episodes = updatedEpisodes,
          apiState = apiState,
          isSelectionMode = selectionMode,
          selectedEpisodeIds = selectedIds,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastUiState(),
      )

  fun onEvent(event: PodcastEvent) {
    when (event) {
      is PodcastEvent.ToggleIsFinished -> {
        viewModelScope.launch { repository.toggleIsFinished(id, event.episode) }
      }
      is PodcastEvent.Download -> {
        downloadRepo.download(event.downloadId, event.url, event.message)
      }
      is PodcastEvent.DeleteDownload -> {
        downloadRepo.delete(event.downloadId)
      }
      is PodcastEvent.SelectionMode -> {
        selectionMode.update { event.isSelectionMode }
        selectedEpisodeIds.update { if (event.isSelectionMode) it + event.itemId else emptySet() }
      }
      is PodcastEvent.SelectItem -> {
        selectedEpisodeIds.update { current ->
          if (current.contains(event.itemId)) {
            current - event.itemId
          } else {
            current + event.itemId
          }
        }
      }
      PodcastEvent.AddEpisode -> {
        apiState.update { PodcastApiState.AddLoading }
        viewModelScope.launch(Dispatchers.IO) { apiState.update { repository.fetchEpisode() } }
      }
      PodcastEvent.ResetAddEpisodeState -> {
        apiState.update { PodcastApiState.Idle }
      }

      is PodcastEvent.DeleteEpisode -> {
        viewModelScope.launch(Dispatchers.IO) {
          val failedIds =
            repository.deleteEpisode(id, event.hardDelete, uiState.value.selectedEpisodeIds)

          if (failedIds.isEmpty()) {
            apiState.update { PodcastApiState.DeleteSuccess(uiState.value.selectedEpisodeIds.size) }
          } else {
            apiState.update {
              val titles =
                uiState.value.episodes
                  .filter { it.episodeId in failedIds }
                  .joinToString { it.title }
              PodcastApiState.DeleteFailure("Failed to delete episodes: $titles")
            }
          }
        }
      }
    }
  }

  private fun startStopSocket(isStart: Boolean) {
    if (isStart) {
      socketManager.connect()
      socketManager.on(SocketEpisode.DOWNLOAD_QUEUED) {
        val obj = it[0] as JSONObject
        val json = json.decodeFromString<PodcastEpisodeDownload>(obj.toString())
        Log.d("SocketManager", "DOWNLOAD_QUEUED: $json")
      }
      socketManager.on(SocketEpisode.DOWNLOAD_STARTED) {
        val obj = it[0] as JSONObject
        val json = json.decodeFromString<PodcastEpisodeDownload>(obj.toString())
        Log.d("SocketManager", "DOWNLOAD_STARTED: $json")
      }
      socketManager.on(SocketEpisode.DOWNLOAD_FINISHED) {
        val obj = it[0] as JSONObject
        val json = json.decodeFromString<PodcastEpisodeDownload>(obj.toString())
        Log.d("SocketManager", "DOWNLOAD_FINISHED: $json")
      }
    } else {
      socketManager.off(SocketEpisode.DOWNLOAD_QUEUED)
      socketManager.off(SocketEpisode.DOWNLOAD_STARTED)
      socketManager.off(SocketEpisode.DOWNLOAD_FINISHED)
      socketManager.disconnect()
    }
  }
}

sealed interface PodcastEvent {
  data class ToggleIsFinished(val episode: Episode) : PodcastEvent

  data class Download(val downloadId: String, val url: String, val message: String) : PodcastEvent

  data class DeleteDownload(val downloadId: String) : PodcastEvent

  data class SelectionMode(val isSelectionMode: Boolean, val itemId: String) : PodcastEvent

  data class SelectItem(val itemId: String) : PodcastEvent

  data class DeleteEpisode(val hardDelete: Boolean) : PodcastEvent

  data object AddEpisode : PodcastEvent

  data object ResetAddEpisodeState : PodcastEvent
}
