package dev.halim.shelfdroid.core.extensions

import java.util.Locale

fun Double.formatChapterTime(padHour: Boolean = false): String {
  val totalSeconds = this.toInt()
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60

  return if (padHour || hours > 0) {
    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
  }
}
