@file:OptIn(ExperimentalTime::class)

package dev.halim.shelfdroid.helper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.extensions.formatChapterTime
import java.util.Locale
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Helper @Inject constructor(private val dataStoreManager: DataStoreManager) {

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.userPrefs.first().accessToken }

  fun generateItemCoverUrl(itemId: String): String {
    return "https://${DataStoreManager.Companion.BASE_URL}/api/items/$itemId/cover"
  }

  suspend fun generateContentUrl(url: String): String =
    "https://${DataStoreManager.Companion.BASE_URL}$url?token=${getToken()}"

  fun generateDownloadId(itemId: String, episodeId: String? = null): String =
    episodeId?.let { "$itemId|$it" } ?: itemId

  fun toReadableDate(long: Long): String {
    val instant = Instant.Companion.fromEpochMilliseconds(long)
    val dateTime = instant.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
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

  fun formatDurationShort(seconds: Double?): String {
    if (seconds == null) return "0s"
    val totalSeconds = seconds.toLong()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60

    return listOfNotNull(
        if (h > 0) "${h}h" else null,
        if (m > 0) "${m}m" else null,
        // Only show seconds if there are no minutes and no hours
        if ((h == 0L && m == 0L) || (s > 0 && h == 0L && m == 0L)) "${s}s" else null,
      )
      .joinToString(" ")
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
    return inputInSeconds.formatChapterTime(padHour)
  }

  /**
   * Formats a session duration into a human-readable string.
   * * Examples:
   * - Same period: "10.00–11.00 AM, 30 January 2026"
   * - Different periods: "10.00 AM–1.00 PM, 30 January 2026"
   * - Different days: "10.00 PM – 2.00 AM, 31 January 2026"
   */
  fun formatSessionTimeRange(
    startedAt: Long,
    updatedAt: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    locale: Locale = Locale.getDefault(),
  ): String {
    val start = Instant.fromEpochMilliseconds(startedAt).toLocalDateTime(timeZone)
    val end = Instant.fromEpochMilliseconds(updatedAt).toLocalDateTime(timeZone)

    fun LocalDateTime.formatTime(): String {
      val h = if (hour % 12 == 0) 12 else hour % 12
      return "$h.${minute.toString().padStart(2, '0')}"
    }

    fun LocalDateTime.amPm() = if (hour < 12) "AM" else "PM"

    fun LocalDateTime.formatDate() =
      "$day ${month.name.lowercase().replaceFirstChar { it.titlecase(locale) }} $year"

    val isSameDay = start.date == end.date

    return if (isSameDay) {
      val startT = start.formatTime()
      val endT = end.formatTime()
      val startP = start.amPm()
      val endP = end.amPm()

      if (startP == endP) "$startT – $endT $startP, ${end.formatDate()}"
      else "$startT $startP–$endT $endP, ${end.formatDate()}"
    } else {
      // Different days: Only show the date for the updatedAt (end) time
      "${start.formatTime()} ${start.amPm()} – ${end.formatTime()} ${end.amPm()}, ${end.formatDate()}"
    }
  }

  fun nowMilis(): Long {
    return System.currentTimeMillis()
  }

  fun createOpenPlayerIntent(mediaId: String, context: Context): PendingIntent =
    createGenericIntent(context, ACTION_OPEN_PLAYER, mediaId)

  fun createOpenDetailIntent(mediaId: String, context: Context): PendingIntent =
    createGenericIntent(context, ACTION_OPEN_DETAIL, mediaId)

  private fun createGenericIntent(
    context: Context,
    action: String,
    mediaId: String,
  ): PendingIntent {
    val intent = Intent(action)
    intent.apply {
      setPackage(context.packageName)
      putExtra(EXTRA_MEDIA_ID, mediaId)
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
  }

  companion object {
    const val ACTION_OPEN_PLAYER = "dev.halim.shelfdroid.OPEN_PLAYER"
    const val ACTION_OPEN_DETAIL = "dev.halim.shelfdroid.OPEN_DETAIL"
    const val EXTRA_MEDIA_ID = "media_id"
  }
}
