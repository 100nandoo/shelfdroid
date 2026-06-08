package dev.halim.shelfdroid.media.service

import androidx.media3.common.Player
import dev.halim.shelfdroid.core.PlayPauseControlState
import javax.inject.Inject

data class PlayerControlSnapshot(
  val isPlaying: Boolean,
  val playWhenReady: Boolean,
  val isLoading: Boolean,
  @Player.State val playbackState: Int,
  val playPauseEnabled: Boolean,
  val showPlayIcon: Boolean,
)

class PlayPauseControlStateMapper @Inject constructor() {
  fun map(snapshot: PlayerControlSnapshot): PlayPauseControlState {
    val showLoadingIndicator =
      snapshot.playWhenReady &&
        (snapshot.isLoading || snapshot.playbackState == Player.STATE_BUFFERING)

    return PlayPauseControlState(
      isPlaying = snapshot.isPlaying,
      enabled = snapshot.playPauseEnabled,
      showPlayIcon = snapshot.showPlayIcon,
      showLoadingIndicator = showLoadingIndicator,
    )
  }
}
