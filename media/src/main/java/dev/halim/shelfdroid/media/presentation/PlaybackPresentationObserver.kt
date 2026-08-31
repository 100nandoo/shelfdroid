package dev.halim.shelfdroid.media.presentation

import androidx.media3.common.Player
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

fun interface PlaybackPresentationObserver {
  suspend fun onPlaybackPresentationChanged()
}

@Singleton
class PlaybackPresentationNotifier
@Inject
constructor(
  private val observers: Set<@JvmSuppressWildcards PlaybackPresentationObserver>
) {
  suspend fun notifyPresentationChanged() {
    observers.forEach { observer ->
      try {
        observer.onPlaybackPresentationChanged()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        // A presentation consumer must not interfere with playback or another consumer.
      }
    }
  }
}

internal val PRESENTATION_VISIBLE_PLAYER_EVENTS =
  setOf(
    Player.EVENT_TIMELINE_CHANGED,
    Player.EVENT_MEDIA_ITEM_TRANSITION,
    Player.EVENT_MEDIA_METADATA_CHANGED,
    Player.EVENT_PLAYBACK_STATE_CHANGED,
    Player.EVENT_PLAY_WHEN_READY_CHANGED,
    Player.EVENT_PLAYER_ERROR,
    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
  )

internal fun Player.Events.hasPresentationVisibleChange(): Boolean =
  PRESENTATION_VISIBLE_PLAYER_EVENTS.any(::contains)
