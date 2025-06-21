package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.util.Locale
import javax.inject.Inject
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Helper @Inject constructor() {
  fun generateItemCoverUrl(itemId: String): String {
    return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/cover"
  }

  fun generateItemStreamUrl(token: String, itemId: String, ino: String): String {
    return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/file/$ino?token=$token"
  }

  fun generateContentUrl(token: String, url: String): String =
    "https://${DataStoreManager.BASE_URL}$url?token=$token"

  fun toReadableDate(long: Long): String {
    val instant = Instant.fromEpochMilliseconds(long)
    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
    return "${dateTime.dayOfMonth} ${dateTime.month.name.lowercase().capitalize(Locale.getDefault())} ${dateTime.year}"
  }

  fun formatDuration(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
      hours > 0 && minutes > 0 -> "$hours hours $minutes minutes"
      hours > 0 -> "$hours hours"
      minutes > 0 -> "$minutes minutes"
      else -> ""
    }
  }

  fun calculateRemaining(duration: Double, progress: Float): String {
    val clampedProgress = progress.coerceIn(0.0f, 1.0f)
    val remainingPercentage = 1.0f - clampedProgress
    val remaining = duration * remainingPercentage
    return formatDuration(remaining)
  }
}
