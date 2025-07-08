package dev.halim.shelfdroid.core.ui.media3

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.shouldEnablePlayPauseButton
import androidx.media3.common.util.Util.shouldShowPlayButton
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.shelfdroid.core.data.screen.player.ExoState
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import dev.halim.shelfdroid.core.ui.player.MediaItemManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class ServiceUiStateHolder
@Inject
constructor(
  private val playerEventListener: Lazy<PlayerEventListener>,
  private val player: Lazy<ExoPlayer>,
  private val playerRepository: PlayerRepository,
  private val mediaItemManager: MediaItemManager,
) {
  val uiState = MutableStateFlow(PlayerUiState())

  fun playContent() {
    player.get().apply {
      val mediaItem = mediaItemManager.toMediaItem(uiState.value)
      val positionMs = uiState.value.currentTime.toLong() * 1000
      setMediaItem(mediaItem, positionMs)
      prepare()
      play()
      collectPlaybackProgress()
      listenIsPlaying()
      listenPlayer()
    }
  }

  private var playbackProgressJob: Job? = null
  private var isPlayingJob: Job? = null
  private var listenPlayer: Job? = null

  private fun listenPlayer() {
    listenPlayer?.cancel()
    listenPlayer =
      playerEventListener
        .get()
        .listen(
          uiState.value,
          changeChapterCallback(),
          { events ->
            handleSeekSliderState(events)
            handleSeekBackState(events)
            handleSeekForwardState(events)
            handlePlayPauseState(events)
          },
        )
  }

  private fun handleSeekSliderState(events: Player.Events) {
    with(player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val multipleButtonState =
          uiState.value.multipleButtonState.copy(
            seekSliderEnabled = isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
          )
        uiState.update { it.copy(multipleButtonState = multipleButtonState) }
      }
    }
  }

  private fun handleSeekBackState(events: Player.Events) {
    with(player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val multipleButtonState =
          uiState.value.multipleButtonState.copy(
            seekBackEnabled = isCommandAvailable(Player.COMMAND_SEEK_BACK)
          )
        uiState.update { it.copy(multipleButtonState = multipleButtonState) }
      }
    }
  }

  private fun handleSeekForwardState(events: Player.Events) {
    with(player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val multipleButtonState =
          uiState.value.multipleButtonState.copy(
            seekForwardEnabled = isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
          )
        uiState.update { it.copy(multipleButtonState = multipleButtonState) }
      }
    }
  }

  @OptIn(UnstableApi::class)
  private fun handlePlayPauseState(events: Player.Events) {
    with(player.get()) {
      if (
        events.containsAny(
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          Player.EVENT_PLAY_WHEN_READY_CHANGED,
          Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
        )
      ) {
        val multipleButtonState =
          uiState.value.multipleButtonState.copy(
            playPauseEnabled = shouldEnablePlayPauseButton(this),
            showPlay = shouldShowPlayButton(this),
          )
        uiState.update { it.copy(multipleButtonState = multipleButtonState) }
      }
    }
  }

  private fun changeChapterCallback() = {
    uiState.update { playerRepository.previousNextChapter(uiState.value, false) }
    playContent()
  }

  private fun collectPlaybackProgress() {
    playbackProgressJob?.cancel()
    playbackProgressJob =
      CoroutineScope(Dispatchers.Main).launch {
        player.get().playbackProgressFlow().collect { raw ->
          uiState.update { playerRepository.toPlayback(uiState.value, raw) }
        }
      }
  }

  private fun listenIsPlaying() {
    isPlayingJob?.cancel()
    isPlayingJob =
      CoroutineScope(Dispatchers.Default).launch {
        player
          .get()
          .addListener(
            object : Player.Listener {
              override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                val exoState = if (isPlaying) ExoState.Playing else ExoState.Pause
                uiState.update { it.copy(exoState = exoState) }
              }
            }
          )
      }
  }
}
