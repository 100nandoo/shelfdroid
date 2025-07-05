package dev.halim.shelfdroid.core.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.player.ChapterPosition
import dev.halim.shelfdroid.core.data.screen.player.ExoState
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.data.screen.player.PlayerState.Hidden
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import dev.halim.shelfdroid.core.ui.media3.MediaIdHolder
import dev.halim.shelfdroid.core.ui.media3.playbackProgressFlow
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
  val player: Lazy<ExoPlayer>,
  private val playerRepository: PlayerRepository,
  private val mediaItemManager: MediaItemManager,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())

  init {
    setupUiState()
  }

  // To retrieve the uiState when user press back on root of stack (activity is destroyed)
  // but there is ongoing playback
  private fun setupUiState() {
    val mediaIdWrapper = MediaIdHolder.mediaIdWrapper
    mediaIdWrapper?.let {
      val itemId = it.itemId
      val episodeId = it.episodeId
      if (episodeId == null) {
        _uiState.value = playerRepository.book(itemId)
      } else {
        _uiState.value = playerRepository.podcast(itemId, episodeId)
      }
      collectPlaybackProgress()
      listenIsPlaying()
      listenPlayer()
    }
  }

  val uiState: StateFlow<PlayerUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        viewModelScope.launch {
          when {
            _uiState.value.id != event.id -> {
              _uiState.update { playerRepository.playBook(event.id) }
              playContent()
            }
            player.get().isPlaying -> pause()
            else -> resume()
          }
        }
      }
      is PlayerEvent.PlayPodcast -> {
        viewModelScope.launch {
          when {
            _uiState.value.episodeId != event.episodeId -> {
              _uiState.update { playerRepository.playPodcast(event.itemId, event.episodeId) }
              playContent()
            }
            player.get().isPlaying -> pause()
            else -> resume()
          }
        }
      }
      is PlayerEvent.ChangeChapter -> {
        _uiState.update { playerRepository.changeChapter(_uiState.value, event.target) }
        playContent()
      }
      is PlayerEvent.SeekTo -> {
        val durationMs = player.get().duration
        _uiState.update { playerRepository.seekTo(_uiState.value, event.target, durationMs) }
        seekTo()
      }
      PlayerEvent.PreviousChapter -> {
        _uiState.update { playerRepository.previousNextChapter(_uiState.value, true) }
        playContent()
      }
      PlayerEvent.NextChapter -> {
        _uiState.update { playerRepository.previousNextChapter(_uiState.value, false) }
        playContent()
      }
      PlayerEvent.Big -> _uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> _uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> _uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> _uiState.update { it.copy(state = Hidden()) }
      PlayerEvent.Logout -> logout()
    }
  }

  private fun playContent() {
    player.get().apply {
      val mediaItem = mediaItemManager.toMediaItem(_uiState.value)
      val positionMs = _uiState.value.currentTime.toLong() * 1000
      setMediaItem(mediaItem, positionMs)
      prepare()
      play()
      collectPlaybackProgress()
      listenIsPlaying()
      listenPlayer()
    }
  }

  private fun pause() {
    player.get().pause()
  }

  private fun resume() {
    player.get().play()
  }

  private fun seekTo() {
    player.get().apply {
      val positionMs = _uiState.value.currentTime.toLong() * 1000
      seekTo(positionMs)
    }
  }

  private fun clearAndStop() {
    player.get().apply {
      MediaIdHolder.reset()
      stop()
      clearMediaItems()
      playbackProgressJob?.cancel()
      isPlayingJob?.cancel()
    }
  }

  private fun logout() {
    _uiState.update { PlayerUiState() }
    clearAndStop()
  }

  private var playbackProgressJob: Job? = null
  private var isPlayingJob: Job? = null
  private var listenPlayer: Job? = null

  private fun listenIsPlaying() {
    isPlayingJob?.cancel()
    isPlayingJob =
      viewModelScope.launch {
        player
          .get()
          .addListener(
            object : Player.Listener {
              override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                val exoState = if (isPlaying) ExoState.Playing else ExoState.Pause
                _uiState.update { it.copy(exoState = exoState) }
              }
            }
          )
      }
  }

  private fun listenPlayer() {
    listenPlayer?.cancel()
    listenPlayer =
      viewModelScope.launch {
        player.get().apply {
          listen { event ->
            if (this.playbackState == Player.STATE_ENDED) {
              val start = _uiState.value.currentChapter?.startTimeSeconds ?: 0.0
              val end = _uiState.value.currentChapter?.endTimeSeconds?.minus(start)?.toLong()
              val isReachEndChapter = end == currentPosition / 1000
              val isNotLastChapter =
                _uiState.value.currentChapter?.chapterPosition != ChapterPosition.Last
              if (isReachEndChapter && isNotLastChapter) {
                onEvent(PlayerEvent.NextChapter)
              }
            }
          }
        }
      }
  }

  private fun collectPlaybackProgress() {
    playbackProgressJob?.cancel()
    playbackProgressJob =
      viewModelScope.launch {
        player.get().playbackProgressFlow().collect { raw ->
          _uiState.update { playerRepository.toPlayback(_uiState.value, raw) }
        }
      }
  }
}

sealed class PlayerEvent {
  class PlayBook(val id: String) : PlayerEvent()

  class PlayPodcast(val itemId: String, val episodeId: String) : PlayerEvent()

  class ChangeChapter(val target: Int) : PlayerEvent()

  class SeekTo(val target: Float) : PlayerEvent()

  data object PreviousChapter : PlayerEvent()

  data object NextChapter : PlayerEvent()

  data object Logout : PlayerEvent()

  data object Big : PlayerEvent()

  data object Small : PlayerEvent()

  data object TempHidden : PlayerEvent()

  data object Hidden : PlayerEvent()
}
