package dev.halim.shelfdroid.widget.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import dev.halim.shelfdroid.media.service.CUSTOM_SLEEP_TIMER
import dev.halim.shelfdroid.media.service.PlaybackService
import kotlinx.coroutines.guava.await

internal class PlaybackWidgetTransportDispatcher {
  suspend fun dispatch(
    action: PlaybackTransportAction,
    connectController: suspend () -> PlaybackWidgetController?,
  ): PlaybackWidgetTransportResult {
    val controller =
      runCatching { connectController() }
        .onFailure { Log.e(TAG, "Failed to connect widget controller", it) }
        .getOrNull() ?: return PlaybackWidgetTransportResult.OpenAppFallback

    return try {
      when (action) {
        PlaybackTransportAction.PlayPause -> {
          if (!controller.isPlayPauseEnabled) return PlaybackWidgetTransportResult.NotAvailable
          if (controller.isPlaying) controller.pause() else controller.play()
          PlaybackWidgetTransportResult.Dispatched
        }
        PlaybackTransportAction.SeekBack -> {
          if (!controller.isSeekBackEnabled) return PlaybackWidgetTransportResult.NotAvailable
          controller.seekBack()
          PlaybackWidgetTransportResult.Dispatched
        }
        PlaybackTransportAction.SeekForward -> {
          if (!controller.isSeekForwardEnabled) return PlaybackWidgetTransportResult.NotAvailable
          controller.seekForward()
          PlaybackWidgetTransportResult.Dispatched
        }
        PlaybackTransportAction.SleepTimer -> {
          if (!controller.isSleepTimerEnabled) return PlaybackWidgetTransportResult.NotAvailable
          controller.toggleSleepTimer()
          PlaybackWidgetTransportResult.Dispatched
        }
      }
    } catch (error: Exception) {
      Log.e(TAG, "Failed to dispatch widget transport action", error)
      PlaybackWidgetTransportResult.OpenAppFallback
    } finally {
      controller.release()
    }
  }

  private companion object {
    const val TAG = "PlaybackWidget"
  }
}

internal enum class PlaybackTransportAction(val parameterValue: String) {
  PlayPause("play_pause"),
  SeekBack("seek_back"),
  SeekForward("seek_forward"),
  SleepTimer("sleep_timer");

  companion object {
    fun fromParameterValue(value: String): PlaybackTransportAction? =
      entries.firstOrNull { it.parameterValue == value }
  }
}

internal sealed interface PlaybackWidgetTransportResult {
  data object Dispatched : PlaybackWidgetTransportResult

  data object NotAvailable : PlaybackWidgetTransportResult

  data object OpenAppFallback : PlaybackWidgetTransportResult
}

internal interface PlaybackWidgetController {
  val isPlaying: Boolean
  val isPlayPauseEnabled: Boolean
  val isSeekBackEnabled: Boolean
  val isSeekForwardEnabled: Boolean
  val isSleepTimerEnabled: Boolean

  fun play()

  fun pause()

  fun seekBack()

  fun seekForward()

  suspend fun toggleSleepTimer()

  fun release()
}

internal object Media3PlaybackWidgetControllerFactory {
  suspend fun connect(context: Context): PlaybackWidgetController? =
    runCatching {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        Media3PlaybackWidgetController(controller)
      }
      .onFailure { Log.e(TAG, "Failed to create widget MediaController", it) }
      .getOrNull()

  private const val TAG = "PlaybackWidget"
}

@OptIn(UnstableApi::class)
private class Media3PlaybackWidgetController(private val controller: MediaController) :
  PlaybackWidgetController {
  private val sleepTimerCommand = SessionCommand(CUSTOM_SLEEP_TIMER, Bundle.EMPTY)

  override val isPlaying: Boolean
    get() = controller.isPlaying

  override val isPlayPauseEnabled: Boolean
    get() = Util.shouldEnablePlayPauseButton(controller)

  override val isSeekBackEnabled: Boolean
    get() = controller.isCommandAvailable(Player.COMMAND_SEEK_BACK)

  override val isSeekForwardEnabled: Boolean
    get() = controller.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)

  override val isSleepTimerEnabled: Boolean
    get() = controller.getAvailableSessionCommands().contains(sleepTimerCommand)

  override fun play() {
    controller.play()
  }

  override fun pause() {
    controller.pause()
  }

  override fun seekBack() {
    controller.seekBack()
  }

  override fun seekForward() {
    controller.seekForward()
  }

  override suspend fun toggleSleepTimer() {
    controller.sendCustomCommand(sleepTimerCommand, Bundle.EMPTY).await()
  }

  override fun release() {
    controller.release()
  }
}
