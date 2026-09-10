package dev.halim.shelfdroid.media.service

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
import javax.inject.Inject
import javax.inject.Singleton

private val CUSTOM_NOTIFICATION_ACTIONS =
  setOf(CUSTOM_SLEEP_TIMER, CUSTOM_NEXT_CHAPTER, CUSTOM_PLAYBACK_SPEED)

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
    return addDisabledNextChapterButton(mediaButtons, mediaButtonPreferences)
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

@UnstableApi
internal fun addDisabledNextChapterButton(
  mediaButtons: ImmutableList<CommandButton>,
  mediaButtonPreferences: ImmutableList<CommandButton>,
): ImmutableList<CommandButton> {
  // Media3 filters disabled preferences before addNotificationActions. NotificationCompat has no
  // disabled action state, so keep the button visible for legacy notifications and rely on the
  // session callback's invalid-state guard to make taps a no-op.
  val disabledNextChapterButton = mediaButtonPreferences.firstOrNull {
    it.sessionCommand?.customAction == CUSTOM_NEXT_CHAPTER && !it.isEnabled
  }
  return if (
    disabledNextChapterButton != null &&
      mediaButtons.none { it.sessionCommand?.customAction == CUSTOM_NEXT_CHAPTER }
  ) {
    val nextPreferenceIndex = mediaButtonPreferences.indexOfFirst {
      it.sessionCommand?.customAction == CUSTOM_NEXT_CHAPTER && !it.isEnabled
    }
    val insertionIndex = mediaButtons.indexOfFirst { button ->
      val preferenceIndex = mediaButtonPreferences.indexOfFirst {
        it.sessionCommand?.customAction == button.sessionCommand?.customAction
      }
      preferenceIndex > nextPreferenceIndex &&
        button.sessionCommand?.customAction in CUSTOM_NOTIFICATION_ACTIONS
    }
    val result = mediaButtons.toMutableList()
    result.add(if (insertionIndex == -1) result.size else insertionIndex, disabledNextChapterButton)
    ImmutableList.copyOf(result)
  } else {
    mediaButtons
  }
}
