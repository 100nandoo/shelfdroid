package dev.halim.shelfdroid.media.notification

import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import kotlin.time.Duration
import dev.halim.shelfdroid.media.playback.controls.NextChapterControlState
import dev.halim.shelfdroid.media.playback.controls.PreviousChapterControlState

internal data class MediaNotificationButtonState(
  val nextChapterState: NextChapterControlState,
  val previousChapterState: PreviousChapterControlState,
  val isSleepTimerActive: Boolean,
  val sleepTimerDuration: Duration,
  val playbackSpeed: Float,
  val notificationPrefs: NotificationPrefs,
  val seekBackSeconds: Int,
  val seekForwardSeconds: Int,
)
