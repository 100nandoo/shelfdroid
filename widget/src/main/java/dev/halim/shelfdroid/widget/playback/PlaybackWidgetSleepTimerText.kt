package dev.halim.shelfdroid.widget.playback

import kotlin.math.ceil
import kotlin.time.Duration

internal fun Duration.toPlaybackWidgetSleepTimerText(): String? {
  if (inWholeSeconds <= 0) return null

  return if (inWholeSeconds < 60) {
    "${inWholeSeconds}s"
  } else {
    "${ceil(inWholeSeconds / 60.0).toInt()}m"
  }
}
