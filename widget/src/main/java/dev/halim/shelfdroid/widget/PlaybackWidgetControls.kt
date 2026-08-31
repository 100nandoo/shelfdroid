package dev.halim.shelfdroid.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.media.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

internal enum class PrimaryPlaybackCommand {
  Play,
  Pause,
  SeekBack,
  SeekForward,
}

internal enum class PlaybackCommandResult {
  Success,
  MissingCurrentPlayback,
  Failed,
}

internal fun interface PlaybackControllerGateway {
  suspend fun dispatch(command: PrimaryPlaybackCommand): PlaybackCommandResult
}

internal fun interface PlaybackWidgetRefreshRequester {
  suspend fun requestRefresh()
}

internal fun interface NormalAppNavigator {
  fun openNormalApp()
}

internal suspend fun handlePlaybackWidgetCommand(
  command: PrimaryPlaybackCommand,
  gateway: PlaybackControllerGateway,
  refreshRequester: PlaybackWidgetRefreshRequester,
  normalAppNavigator: NormalAppNavigator,
) {
  val result =
    try {
      gateway.dispatch(command)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (_: Exception) {
      PlaybackCommandResult.Failed
    }

  when (result) {
    PlaybackCommandResult.Success -> refreshRequester.requestRefresh()
    PlaybackCommandResult.MissingCurrentPlayback -> normalAppNavigator.openNormalApp()
    PlaybackCommandResult.Failed -> Unit
  }
}

@Singleton
internal class PlaybackWidgetCommandHandler
@Inject
constructor(
  private val gateway: Media3PlaybackControllerGateway,
  private val refreshRequester: GlancePlaybackWidgetRefreshRequester,
  private val normalAppNavigator: ShelfDroidNormalAppNavigator,
) {
  suspend fun handle(command: PrimaryPlaybackCommand) {
    handlePlaybackWidgetCommand(command, gateway, refreshRequester, normalAppNavigator)
  }
}

@Singleton
internal class Media3PlaybackControllerGateway
@Inject
constructor(@param:ApplicationContext private val context: Context) : PlaybackControllerGateway {
  @OptIn(UnstableApi::class)
  override suspend fun dispatch(command: PrimaryPlaybackCommand): PlaybackCommandResult =
    withContext(Dispatchers.Main.immediate) {
      val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
      val controllerFuture = MediaController.Builder(context, token).buildAsync()
      try {
        val controller = controllerFuture.await()
        if (controller.currentMediaItem == null) {
          return@withContext PlaybackCommandResult.MissingCurrentPlayback
        }
        if (controller.playerError != null || !controller.isCommandAvailable(command.playerCommand)) {
          return@withContext PlaybackCommandResult.Failed
        }

        when (command) {
          PrimaryPlaybackCommand.Play -> controller.play()
          PrimaryPlaybackCommand.Pause -> controller.pause()
          PrimaryPlaybackCommand.SeekBack -> controller.seekBack()
          PrimaryPlaybackCommand.SeekForward -> controller.seekForward()
        }
        PlaybackCommandResult.Success
      } finally {
        MediaController.releaseFuture(controllerFuture)
      }
    }
}

private val PrimaryPlaybackCommand.playerCommand: Int
  get() =
    when (this) {
      PrimaryPlaybackCommand.Play,
      PrimaryPlaybackCommand.Pause -> Player.COMMAND_PLAY_PAUSE
      PrimaryPlaybackCommand.SeekBack -> Player.COMMAND_SEEK_BACK
      PrimaryPlaybackCommand.SeekForward -> Player.COMMAND_SEEK_FORWARD
    }

@Singleton
internal class GlancePlaybackWidgetRefreshRequester
@Inject
constructor(@param:ApplicationContext private val context: Context) :
  PlaybackWidgetRefreshRequester {
  override suspend fun requestRefresh() {
    PlaybackWidget().updateAll(context)
  }
}

@Singleton
internal class ShelfDroidNormalAppNavigator
@Inject
constructor(@param:ApplicationContext private val context: Context) : NormalAppNavigator {
  override fun openNormalApp() {
    context.startActivity(
      Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }
}

public class PlayPlaybackAction : PlaybackWidgetAction(PrimaryPlaybackCommand.Play)

public class PausePlaybackAction : PlaybackWidgetAction(PrimaryPlaybackCommand.Pause)

public class SeekBackPlaybackAction : PlaybackWidgetAction(PrimaryPlaybackCommand.SeekBack)

public class SeekForwardPlaybackAction : PlaybackWidgetAction(PrimaryPlaybackCommand.SeekForward)

public abstract class PlaybackWidgetAction internal constructor(
  private val command: PrimaryPlaybackCommand,
) : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    playbackWidgetEntryPoint(context).commandHandler().handle(command)
  }
}
