package dev.halim.shelfdroid.media.notification

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.prefs.DEFAULT_SEEK_INTERVAL_SECONDS
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.media.playback.controls.NextChapterControlState
import dev.halim.shelfdroid.media.playback.controls.PreviousChapterControlState
import dev.halim.shelfdroid.media.playback.controls.nextChapterControlState
import dev.halim.shelfdroid.media.playback.controls.previousChapterControlState
import dev.halim.shelfdroid.media.session.CUSTOM_BACK
import dev.halim.shelfdroid.media.session.CUSTOM_FORWARD
import dev.halim.shelfdroid.media.session.CUSTOM_NEXT_CHAPTER
import dev.halim.shelfdroid.media.session.CUSTOM_PLAYBACK_SPEED
import dev.halim.shelfdroid.media.session.CUSTOM_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.media.session.CUSTOM_SLEEP_TIMER

internal val CUSTOM_NOTIFICATION_ACTIONS =
  setOf(
    CUSTOM_SLEEP_TIMER,
    CUSTOM_NEXT_CHAPTER,
    CUSTOM_PREVIOUS_CHAPTER,
    CUSTOM_PLAYBACK_SPEED,
  )

@UnstableApi
internal object MediaNotificationButtons {
  val BACK_COMMAND_BUTTON: CommandButton = seekBackCommandButton(DEFAULT_SEEK_INTERVAL_SECONDS)

  val FORWARD_COMMAND_BUTTON: CommandButton =
    seekForwardCommandButton(DEFAULT_SEEK_INTERVAL_SECONDS)

  val SLEEP_TIMER_OFF_BUTTON: CommandButton =
    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
      .setCustomIconResId(CoreR.drawable.timer_off)
      .setSessionCommand(SessionCommand(CUSTOM_SLEEP_TIMER, Bundle()))
      .setDisplayName("Start sleep timer")
      .build()

  val SLEEP_TIMER_ON_BUTTON: CommandButton =
    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
      .setCustomIconResId(CoreR.drawable.timer)
      .setSessionCommand(SessionCommand(CUSTOM_SLEEP_TIMER, Bundle()))
      .setDisplayName("Cancel sleep timer")
      .build()

  val PLAYBACK_SPEED_BUTTON: CommandButton = playbackSpeedCommandButton(1f)
}

@UnstableApi
internal fun seekBackCommandButton(seconds: Int): CommandButton =
  CommandButton.Builder(CommandButton.ICON_REWIND)
    .setSessionCommand(SessionCommand(CUSTOM_BACK, Bundle()))
    .setDisplayName("Rewind ${seconds}s")
    .build()

@UnstableApi
internal fun seekForwardCommandButton(seconds: Int): CommandButton =
  CommandButton.Builder(CommandButton.ICON_FAST_FORWARD)
    .setSessionCommand(SessionCommand(CUSTOM_FORWARD, Bundle()))
    .setDisplayName("Forward ${seconds}s")
    .build()

internal fun playbackSpeedIconResId(playbackSpeed: Float): Int =
  when (playbackSpeed) {
    0.5f -> CoreR.drawable.speed_0_5x
    0.75f -> CoreR.drawable.speed_0_75
    1f -> CoreR.drawable.speed
    1.25f -> CoreR.drawable.speed_1_25
    1.5f -> CoreR.drawable.speed_1_5
    1.75f -> CoreR.drawable.speed_1_75
    2f -> CoreR.drawable.speed_2x
    else -> CoreR.drawable.speed
  }

@UnstableApi
internal fun playbackSpeedCommandButton(playbackSpeed: Float): CommandButton =
  CommandButton.Builder(CommandButton.ICON_UNDEFINED)
    .setCustomIconResId(playbackSpeedIconResId(playbackSpeed))
    .setSessionCommand(SessionCommand(CUSTOM_PLAYBACK_SPEED, Bundle()))
    .setDisplayName("Playback speed")
    .build()

@UnstableApi
internal fun nextChapterCommandButton(
  displayName: CharSequence,
  isEnabled: Boolean,
): CommandButton =
  CommandButton.Builder(CommandButton.ICON_UNDEFINED)
    .setCustomIconResId(CoreR.drawable.skip_next)
    .setSessionCommand(SessionCommand(CUSTOM_NEXT_CHAPTER, Bundle()))
    .setDisplayName(displayName)
    .setEnabled(isEnabled)
    .build()

@UnstableApi
internal fun previousChapterCommandButton(
  displayName: CharSequence,
  isEnabled: Boolean,
): CommandButton =
  CommandButton.Builder(CommandButton.ICON_UNDEFINED)
    .setCustomIconResId(CoreR.drawable.skip_previous)
    .setSessionCommand(SessionCommand(CUSTOM_PREVIOUS_CHAPTER, Bundle()))
    .setDisplayName(displayName)
    .setEnabled(isEnabled)
    .build()

@UnstableApi
fun mediaNotificationButtons(
  nextChapterDisplayName: CharSequence,
  uiState: PlayerUiState,
  isChapterTransitioning: Boolean,
  isSleepTimerActive: Boolean,
  notificationPrefs: NotificationPrefs = NotificationPrefs(),
  seekBackSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  seekForwardSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  previousChapterDisplayName: CharSequence = "Previous chapter",
): List<CommandButton> =
  mediaNotificationButtons(
    nextChapterDisplayName = nextChapterDisplayName,
    nextChapterState = nextChapterControlState(uiState, isChapterTransitioning),
    isSleepTimerActive = isSleepTimerActive,
    notificationPrefs = notificationPrefs,
    currentPlaybackSpeed = uiState.advancedControl.speed,
    seekBackSeconds = seekBackSeconds,
    seekForwardSeconds = seekForwardSeconds,
    previousChapterDisplayName = previousChapterDisplayName,
    previousChapterState = previousChapterControlState(uiState, isChapterTransitioning),
  )

@UnstableApi
fun mediaNotificationButtons(
  nextChapterDisplayName: CharSequence,
  nextChapterState: NextChapterControlState,
  isSleepTimerActive: Boolean,
  notificationPrefs: NotificationPrefs = NotificationPrefs(),
  currentPlaybackSpeed: Float = 1f,
  seekBackSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  seekForwardSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  previousChapterDisplayName: CharSequence = "Previous chapter",
  previousChapterState: PreviousChapterControlState =
    PreviousChapterControlState(visible = false, enabled = false),
): List<CommandButton> {
  return buildList {
    add(seekBackCommandButton(seekBackSeconds))
    add(seekForwardCommandButton(seekForwardSeconds))
    val actions = listOf(notificationPrefs.firstAction, notificationPrefs.secondAction).distinct()
    actions.forEach { action ->
      when (action) {
        MediaNotificationAction.SleepTimer ->
          add(
            if (isSleepTimerActive) {
              MediaNotificationButtons.SLEEP_TIMER_ON_BUTTON
            } else {
              MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON
            }
          )
        MediaNotificationAction.NextChapter ->
          if (nextChapterState.visible) {
            add(nextChapterCommandButton(nextChapterDisplayName, nextChapterState.enabled))
          }
        MediaNotificationAction.PreviousChapter ->
          if (previousChapterState.visible) {
            add(
              previousChapterCommandButton(
                previousChapterDisplayName,
                previousChapterState.enabled,
              )
            )
          }
        MediaNotificationAction.PlaybackSpeed ->
          add(playbackSpeedCommandButton(currentPlaybackSpeed))
        MediaNotificationAction.None -> Unit
      }
    }
  }
}

@UnstableApi
internal fun addDisabledChapterButtons(
  mediaButtons: ImmutableList<CommandButton>,
  mediaButtonPreferences: ImmutableList<CommandButton>,
  chapterActions: Set<String> = setOf(CUSTOM_NEXT_CHAPTER, CUSTOM_PREVIOUS_CHAPTER),
): ImmutableList<CommandButton> {
  // Media3 filters disabled preferences before addNotificationActions. NotificationCompat has no
  // disabled action state, so keep the button visible and rely on the session callback's
  // invalid-state guard to make taps a no-op.
  val result = mediaButtons.toMutableList()
  mediaButtonPreferences.forEachIndexed { preferenceIndex, preference ->
    val customAction = preference.sessionCommand?.customAction
    if (
      customAction in chapterActions &&
        !preference.isEnabled &&
        result.none { it.sessionCommand?.customAction == customAction }
    ) {
      val insertionIndex = result.indexOfFirst { button ->
        val buttonPreferenceIndex = mediaButtonPreferences.indexOfFirst {
          it.sessionCommand?.customAction == button.sessionCommand?.customAction
        }
        buttonPreferenceIndex > preferenceIndex &&
          button.sessionCommand?.customAction in CUSTOM_NOTIFICATION_ACTIONS
      }
      result.add(if (insertionIndex == -1) result.size else insertionIndex, preference)
    }
  }
  return ImmutableList.copyOf(result)
}
