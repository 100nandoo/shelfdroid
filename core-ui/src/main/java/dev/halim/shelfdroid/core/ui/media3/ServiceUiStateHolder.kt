package dev.halim.shelfdroid.core.ui.media3

import androidx.media3.common.Player
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
  private val playerEventListener: PlayerEventListener,
  private val player: Lazy<ExoPlayer>,
  private val playerRepository: PlayerRepository,
  private val mediaItemManager: MediaItemManager,
) {
  val uiState = MutableStateFlow(PlayerUiState())

  private var playbackProgressJob: Job? = null
  private var isPlayingJob: Job? = null
  private var listenPlayer: Job? = null

  private fun listenPlayer() {
    listenPlayer?.cancel()
    listenPlayer =
      playerEventListener.listen(
        player.get(),
        uiState.value,
        {
          uiState.update { playerRepository.previousNextChapter(uiState.value, false) }
          playContent()
        },
      )
  }

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
