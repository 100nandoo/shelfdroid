package dev.halim.shelfdroid.core.ui.media3

import androidx.media3.common.Player
import dev.halim.shelfdroid.core.data.screen.player.RawPlaybackProgress
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun Player.playbackProgressFlow(): Flow<RawPlaybackProgress> = flow {
  while (true) {
    emit(
      RawPlaybackProgress(
        position = currentPosition,
        duration = duration,
        bufferedPosition = bufferedPosition,
      )
    )

    delay(1.seconds)
  }
}
