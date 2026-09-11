package dev.halim.shelfdroid.media.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import dev.halim.shelfdroid.media.R
import dev.halim.shelfdroid.media.session.CUSTOM_BACK
import dev.halim.shelfdroid.media.session.CUSTOM_FORWARD
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class CustomMediaNotificationProvider @Inject constructor(context: Context) :
  DefaultMediaNotificationProvider(context) {

  init {
    setSmallIcon(R.drawable.ic_notification)
  }

  override fun getMediaButtons(
    session: MediaSession,
    playerCommands: Player.Commands,
    mediaButtonPreferences: ImmutableList<CommandButton>,
    showPauseButton: Boolean,
  ): ImmutableList<CommandButton> {
    val mediaButtons =
      super.getMediaButtons(
        session,
        playerCommands,
        mediaButtonPreferences,
        showPauseButton,
      )
    return addDisabledChapterButtons(mediaButtons, mediaButtonPreferences)
  }

  override fun addNotificationActions(
    mediaSession: MediaSession,
    mediaButtons: ImmutableList<CommandButton>,
    builder: NotificationCompat.Builder,
    actionFactory: MediaNotification.ActionFactory,
  ): IntArray {
    val playPauseButton = mediaButtons.firstOrNull { it.playerCommand == Player.COMMAND_PLAY_PAUSE }
    val backButton =
      mediaButtons.firstOrNull { it.sessionCommand?.customAction == CUSTOM_BACK }
        ?: MediaNotificationButtons.BACK_COMMAND_BUTTON
    val forwardButton =
      mediaButtons.firstOrNull { it.sessionCommand?.customAction == CUSTOM_FORWARD }
        ?: MediaNotificationButtons.FORWARD_COMMAND_BUTTON
    val customButtons = mediaButtons.filter {
      it.sessionCommand?.customAction in CUSTOM_NOTIFICATION_ACTIONS
    }

    val notificationButtons =
      if (playPauseButton != null) {
        backButton.extras.putInt(
          COMMAND_KEY_COMPACT_VIEW_INDEX,
          0,
        )
        playPauseButton.extras.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1)
        forwardButton.extras.putInt(
          COMMAND_KEY_COMPACT_VIEW_INDEX,
          2,
        )

        ImmutableList.builder<CommandButton>()
          .add(backButton)
          .add(playPauseButton)
          .add(forwardButton)
          .addAll(customButtons)
          .build()
      } else {
        mediaButtons
      }

    return super.addNotificationActions(mediaSession, notificationButtons, builder, actionFactory)
  }
}
