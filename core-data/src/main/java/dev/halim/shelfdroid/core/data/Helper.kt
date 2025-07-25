package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.util.Locale
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Helper @Inject constructor(private val dataStoreManager: DataStoreManager) {

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.userPrefs.first().accessToken }

  fun generateItemCoverUrl(itemId: String): String {
    return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/cover"
  }

  suspend fun generateContentUrl(url: String): String =
    "https://${DataStoreManager.BASE_URL}$url?token=${getToken()}"

  fun generateDownloadId(itemId: String, episodeId: String?): String =
    episodeId?.let { "$itemId|$it" } ?: itemId

  @OptIn(ExperimentalTime::class)
  fun toReadableDate(long: Long): String {
    val instant = Instant.fromEpochMilliseconds(long)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.day} ${dateTime.month.name.lowercase()
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${dateTime.year}"
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

  /**
   * Formats a time duration given in seconds into a human-readable string.
   *
   * @param inputInSeconds The time duration in seconds.
   * @param padHour If true, always include the hour field (e.g. "00:01:05"). If false, omit hours
   *   when zero (e.g. "01:05").
   * @return A formatted time string in "HH:MM:SS" or "MM:SS" format.
   *
   * Examples:
   * - formatTime(65.0) -> "01:05"
   * - formatTime(3661.0) -> "01:01:01"
   * - formatTime(59.0) -> "00:59"
   * - formatTime(59.0, padHour = true) -> "00:00:59"
   * - formatTime(3600.0) -> "01:00:00"
   */
  fun calculateRemaining(duration: Double, progress: Float): String {
    val clampedProgress = progress.coerceIn(0.0f, 1.0f)
    val remainingPercentage = 1.0f - clampedProgress
    val remaining = duration * remainingPercentage
    return formatDuration(remaining)
  }

  fun formatChapterTime(inputInSeconds: Double, padHour: Boolean = false): String {
    val totalSeconds = inputInSeconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (padHour || hours > 0) {
      String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
  }
}
