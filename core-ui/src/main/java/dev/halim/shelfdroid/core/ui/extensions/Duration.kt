package dev.halim.shelfdroid.core.ui.extensions

import kotlin.math.ceil
import kotlin.time.Duration

fun Duration.toSleepTimerText(): String {
  return if (this.inWholeSeconds < 60) {
    "${this.inWholeSeconds}s"
  } else {
    val minutes = ceil(this.inWholeSeconds / 60.0).toInt()
    "${minutes}m"
  }
}
