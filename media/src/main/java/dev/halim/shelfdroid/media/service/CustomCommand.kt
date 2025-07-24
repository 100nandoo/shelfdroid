package dev.halim.shelfdroid.media.service

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import dev.halim.shelfdroid.media.R
import javax.inject.Inject
import javax.inject.Singleton

const val CUSTOM_BACK = "CUSTOM_BACK"
const val CUSTOM_FORWARD = "CUSTOM_FORWARD"

@UnstableApi
@Singleton
class CustomMediaNotificationProvider @Inject constructor(context: Context) :
  DefaultMediaNotificationProvider(context) {

  init {
    setSmallIcon(R.drawable.ic_notification)
  }

  override fun addNotificationActions(
    mediaSession: MediaSession,
    mediaButtons: ImmutableList<CommandButton>,
    builder: NotificationCompat.Builder,
    actionFactory: MediaNotification.ActionFactory,
  ): IntArray {
    val playPauseButton = mediaButtons.firstOrNull { it.playerCommand == Player.COMMAND_PLAY_PAUSE }

    val notificationButtons =
      if (playPauseButton != null) {
        playPauseButton.extras.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1)

        ImmutableList.of(BACK_COMMAND_BUTTON, playPauseButton, FORWARD_COMMAND_BUTTON)
      } else {
        mediaButtons
      }

    return super.addNotificationActions(mediaSession, notificationButtons, builder, actionFactory)
  }

  companion object {
    @UnstableApi
    val BACK_COMMAND_BUTTON: CommandButton =
      CommandButton.Builder(CommandButton.ICON_REWIND)
        .setSessionCommand(SessionCommand(CUSTOM_BACK, Bundle.EMPTY))
        .setDisplayName("Rewind 10s")
        .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 0) })
        .build()

    @UnstableApi
    val FORWARD_COMMAND_BUTTON: CommandButton =
      CommandButton.Builder(CommandButton.ICON_FAST_FORWARD)
        .setSessionCommand(SessionCommand(CUSTOM_FORWARD, Bundle.EMPTY))
        .setDisplayName("Forward 10s")
        .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 2) })
        .build()
  }
}
