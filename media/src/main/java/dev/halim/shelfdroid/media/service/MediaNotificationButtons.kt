package dev.halim.shelfdroid.media.service

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.R as CoreR

const val CUSTOM_BACK = "CUSTOM_BACK"
const val CUSTOM_FORWARD = "CUSTOM_FORWARD"
const val CUSTOM_SLEEP_TIMER = "CUSTOM_SLEEP_TIMER"
const val CUSTOM_NEXT_CHAPTER = "CUSTOM_NEXT_CHAPTER"

@UnstableApi
internal object MediaNotificationButtons {
  val BACK_COMMAND_BUTTON: CommandButton =
    CommandButton.Builder(CommandButton.ICON_REWIND)
      .setSessionCommand(SessionCommand(CUSTOM_BACK, Bundle()))
      .setDisplayName("Rewind 10s")
      .build()

  val FORWARD_COMMAND_BUTTON: CommandButton =
    CommandButton.Builder(CommandButton.ICON_FAST_FORWARD)
      .setSessionCommand(SessionCommand(CUSTOM_FORWARD, Bundle()))
      .setDisplayName("Forward 10s")
      .build()

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
}

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
fun mediaNotificationButtons(
  nextChapterDisplayName: CharSequence,
  uiState: PlayerUiState,
  isChapterTransitioning: Boolean,
  isSleepTimerActive: Boolean,
): List<CommandButton> =
  mediaNotificationButtons(
    nextChapterDisplayName,
    nextChapterControlState(uiState, isChapterTransitioning),
    isSleepTimerActive,
  )

@UnstableApi
fun mediaNotificationButtons(
  nextChapterDisplayName: CharSequence,
  nextChapterState: NextChapterControlState,
  isSleepTimerActive: Boolean,
): List<CommandButton> {
  return buildList {
    add(MediaNotificationButtons.BACK_COMMAND_BUTTON)
    add(MediaNotificationButtons.FORWARD_COMMAND_BUTTON)
    add(
      if (isSleepTimerActive) {
        MediaNotificationButtons.SLEEP_TIMER_ON_BUTTON
      } else {
        MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON
      }
    )
    if (nextChapterState.visible) {
      add(nextChapterCommandButton(nextChapterDisplayName, nextChapterState.enabled))
    }
  }
}
