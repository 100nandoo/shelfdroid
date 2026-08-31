package dev.halim.shelfdroid.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.media.service.CUSTOM_NEXT_CHAPTER
import dev.halim.shelfdroid.media.service.CUSTOM_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.media.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

internal sealed interface PlaybackWidgetCommand

internal enum class PrimaryPlaybackCommand : PlaybackWidgetCommand {
  Play,
  Pause,
  SeekBack,
  SeekForward,
}

internal enum class ChapterPlaybackCommand(val customAction: String) : PlaybackWidgetCommand {
  Previous(CUSTOM_PREVIOUS_CHAPTER),
  Next(CUSTOM_NEXT_CHAPTER),
}

internal enum class PlaybackCommandResult {
  Success,
  MissingCurrentPlayback,
  Failed,
}

internal fun interface PlaybackControllerGateway {
  suspend fun dispatch(command: PlaybackWidgetCommand): PlaybackCommandResult
}

internal fun interface PlaybackWidgetRefreshRequester {
  suspend fun requestAllInstancesRefresh()
}

internal fun interface NormalAppNavigator {
  fun openNormalApp()
}

internal suspend fun handlePlaybackWidgetCommand(
  command: PlaybackWidgetCommand,
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
    PlaybackCommandResult.Success -> refreshRequester.requestAllInstancesRefresh()
    PlaybackCommandResult.MissingCurrentPlayback -> normalAppNavigator.openNormalApp()
    PlaybackCommandResult.Failed -> Unit
  }
}

@Singleton
internal class PlaybackWidgetCommandHandler
@Inject
constructor(
  private val gateway: Media3PlaybackControllerGateway,
  private val refreshRequester: PlaybackWidgetRefreshRequester,
  private val normalAppNavigator: ShelfDroidNormalAppNavigator,
) {
  suspend fun handle(command: PlaybackWidgetCommand) {
    handlePlaybackWidgetCommand(command, gateway, refreshRequester, normalAppNavigator)
  }
}

@Singleton
internal class Media3PlaybackControllerGateway
@Inject
constructor(@param:ApplicationContext private val context: Context) : PlaybackControllerGateway {
  @OptIn(UnstableApi::class)
  override suspend fun dispatch(command: PlaybackWidgetCommand): PlaybackCommandResult =
    withContext(Dispatchers.Main.immediate) {
      val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
      val controllerFuture = MediaController.Builder(context, token).buildAsync()
      try {
        val controller = controllerFuture.await()
        if (controller.currentMediaItem == null) {
          return@withContext PlaybackCommandResult.MissingCurrentPlayback
        }
        if (controller.playerError != null) {
          return@withContext PlaybackCommandResult.Failed
        }

        when (command) {
          is PrimaryPlaybackCommand -> {
            if (!controller.isCommandAvailable(command.playerCommand)) {
              return@withContext PlaybackCommandResult.Failed
            }
            when (command) {
              PrimaryPlaybackCommand.Play -> controller.play()
              PrimaryPlaybackCommand.Pause -> controller.pause()
              PrimaryPlaybackCommand.SeekBack -> controller.seekBack()
              PrimaryPlaybackCommand.SeekForward -> controller.seekForward()
            }
          }
          is ChapterPlaybackCommand -> {
            val sessionCommand = SessionCommand(command.customAction, Bundle.EMPTY)
            if (!controller.isSessionCommandAvailable(sessionCommand)) {
              return@withContext PlaybackCommandResult.Failed
            }
            val result = controller.sendCustomCommand(sessionCommand, Bundle.EMPTY).await()
            if (result.resultCode != SessionResult.RESULT_SUCCESS) {
              return@withContext PlaybackCommandResult.Failed
            }
          }
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
  override suspend fun requestAllInstancesRefresh() {
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

public class PreviousChapterPlaybackAction :
  PlaybackWidgetAction(ChapterPlaybackCommand.Previous)

public class NextChapterPlaybackAction : PlaybackWidgetAction(ChapterPlaybackCommand.Next)

public abstract class PlaybackWidgetAction internal constructor(
  private val command: PlaybackWidgetCommand,
) : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    playbackWidgetEntryPoint(context).commandHandler().handle(command)
  }
}
