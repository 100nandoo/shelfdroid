package dev.halim.shelfdroid.core.ui.screen.podcast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState.DeleteFailure
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState.DeleteSuccess
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState.OpenRssFeedFailure
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.navigation.Podcast
import dev.halim.shelfdroid.core.ui.player.forItemAction
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.media.service.PlayerStore
import dev.halim.socketio.SocketManager
import dev.halim.socketio.SocketManager.Event.Episode as SocketEpisode
import dev.halim.socketio.model.PodcastEpisodeDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

@HiltViewModel(assistedFactory = PodcastViewModel.Factory::class)
class PodcastViewModel
@AssistedInject
constructor(
  private val prefsRepository: PrefsRepository,
  private val repository: PodcastRepository,
  private val downloadRepo: DownloadRepo,
  private val socketManager: SocketManager,
  private val json: Json,
  playerStore: PlayerStore,
  @Assisted navKey: Podcast,
) : ViewModel() {
  val id: String = navKey.id
  private val apiState = MutableStateFlow<PodcastApiState>(PodcastApiState.Idle)
  private val interactionState = MutableStateFlow(PodcastInteractionState())

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
        playerStore.uiState,
        apiState,
        interactionState,
      ) { podcast, playerState, apiState, interactionState ->
        val updatedEpisodes =
          podcast.episodes.map { episode ->
            if (episode.episodeId == playerState.episodeId) {
              episode.copy(
                progress = playerState.playbackProgress.progress,
                playPause = playerState.playPause.forItemAction(true),
              )
            } else episode.copy(playPause = playerState.playPause.forItemAction(false))
          }
        podcast.copy(
          episodes = updatedEpisodes,
          apiState = apiState,
          isSelectionMode = interactionState.isSelectionMode,
          selectedEpisodeIds = interactionState.selectedEpisodeIds,
          actionSheetEpisodeId = interactionState.actionSheetEpisodeId,
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
        viewModelScope.launch { downloadRepo.downloadPodcastEpisode(event.download) }
      }
      is PodcastEvent.DeleteDownload -> {
        viewModelScope.launch { downloadRepo.deletePodcastEpisode(event.download) }
      }
      is PodcastEvent.OpenEpisodeActions -> {
        interactionState.update {
          it.openEpisodeActions(
            episodeId = event.episodeId,
            canEdit = uiState.value.canEditEpisode,
            canDelete = uiState.value.canDeleteEpisode,
          )
        }
      }
      PodcastEvent.DismissEpisodeActions -> {
        interactionState.update { it.dismissEpisodeActions() }
      }
      is PodcastEvent.SelectionMode -> {
        viewModelScope.launch {
          interactionState.update {
            it.setSelectionMode(
              enabled = event.isSelectionMode,
              episodeId = event.itemId,
              autoSelectedIds = autoSelectedFinishedIds(),
            )
          }
        }
      }
      is PodcastEvent.SelectItem -> {
        interactionState.update { it.toggleSelection(event.itemId) }
      }
      is PodcastEvent.StartDeleteSelection -> {
        viewModelScope.launch {
          interactionState.update {
            it.startDeleteSelectionFromActions(
              episodeId = event.episodeId,
              autoSelectedIds = autoSelectedFinishedIds(),
            )
          }
        }
      }
      is PodcastEvent.DeleteEpisode -> {
        viewModelScope.launch(Dispatchers.IO) {
          val failedIds =
            repository.deleteEpisode(id, event.hardDelete, uiState.value.selectedEpisodeIds)

          if (failedIds.isEmpty()) {
            apiState.update { DeleteSuccess(uiState.value.selectedEpisodeIds.size) }
          } else {
            apiState.update {
              val titles =
                uiState.value.episodes
                  .filter { it.episodeId in failedIds }
                  .joinToString { it.title }
              DeleteFailure("Failed to delete episodes: $titles")
            }
          }
        }
      }

      is PodcastEvent.SwitchAutoSelectFinished -> {
        viewModelScope.launch {
          val ids = finishedIds()
          interactionState.update { current ->
            current.copy(
              selectedEpisodeIds =
                if (event.enabled) current.selectedEpisodeIds + ids
                else current.selectedEpisodeIds - ids
            )
          }
          prefsRepository.updateAutoSelectFinished(event.enabled)
        }
      }
      PodcastEvent.AddEpisode -> {
        apiState.update { PodcastApiState.AddLoading }
        viewModelScope.launch(Dispatchers.IO) { apiState.update { repository.fetchEpisode() } }
      }
      is PodcastEvent.OpenGeneratedRssFeed -> {
        apiState.update { PodcastApiState.OpenRssFeedLoading }
        viewModelScope.launch(Dispatchers.IO) {
          val result =
            repository.openGeneratedRssFeed(
              itemId = id,
              slug = event.slug,
              preventIndexing = event.preventIndexing,
              ownerName = event.ownerName,
              ownerEmail = event.ownerEmail,
            )
          apiState.update {
            result.fold(
              onSuccess = { PodcastApiState.OpenRssFeedSuccess },
              onFailure = {
                OpenRssFeedFailure(it.message ?: "Failed to open generated RSS feed")
              },
            )
          }
        }
      }
      is PodcastEvent.CloseGeneratedRssFeed -> {
        apiState.update { PodcastApiState.CloseRssFeedLoading }
        viewModelScope.launch(Dispatchers.IO) {
          val result = repository.closeGeneratedRssFeed(itemId = id, feedId = event.feedId)
          apiState.update {
            result.fold(
              onSuccess = { PodcastApiState.CloseRssFeedSuccess },
              onFailure = {
                PodcastApiState.CloseRssFeedFailure(
                  it.message ?: "Failed to close generated RSS feed"
                )
              },
            )
          }
        }
      }
      PodcastEvent.ResetApiState -> {
        apiState.update { PodcastApiState.Idle }
      }
    }
  }

  private fun finishedIds(): Set<String> {
    return uiState.value.episodes.filter { it.isFinished }.map { it.episodeId }.toSet()
  }

  private suspend fun autoSelectedFinishedIds(): Set<String> {
    return if (prefsRepository.crudPrefs.first().episodeAutoSelectFinished) finishedIds()
    else emptySet()
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

  @AssistedFactory
  interface Factory {
    fun create(navKey: Podcast): PodcastViewModel
  }
}

sealed interface PodcastEvent {
  data class ToggleIsFinished(val episode: Episode) : PodcastEvent

  data class Download(val download: DownloadUiState) : PodcastEvent

  data class DeleteDownload(val download: DownloadUiState) : PodcastEvent

  data class OpenEpisodeActions(val episodeId: String) : PodcastEvent

  data object DismissEpisodeActions : PodcastEvent

  data class SelectionMode(val isSelectionMode: Boolean, val itemId: String) : PodcastEvent

  data class SelectItem(val itemId: String) : PodcastEvent

  data class StartDeleteSelection(val episodeId: String) : PodcastEvent

  data class DeleteEpisode(val hardDelete: Boolean) : PodcastEvent

  data class SwitchAutoSelectFinished(val enabled: Boolean) : PodcastEvent

  data object AddEpisode : PodcastEvent

  data class OpenGeneratedRssFeed(
    val slug: String,
    val preventIndexing: Boolean,
    val ownerName: String,
    val ownerEmail: String,
  ) : PodcastEvent

  data class CloseGeneratedRssFeed(val feedId: String) : PodcastEvent

  data object ResetApiState : PodcastEvent
}
