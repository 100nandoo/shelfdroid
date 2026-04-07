@file:OptIn(ExperimentalTime::class)

package dev.halim.shelfdroid.helper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.extensions.formatChapterTime
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Instant.Companion.parse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class Helper
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  @ApplicationContext private val context: Context,
) {

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.userPrefs.first().accessToken }

  fun generateItemCoverUrl(itemId: String): String {
    return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/cover"
  }

  suspend fun generateContentUrl(url: String): String =
    "https://${DataStoreManager.BASE_URL}$url?token=${getToken()}"

  suspend fun generateBackupDownloadUrl(backupId: String): String =
    "https://${DataStoreManager.BASE_URL}/api/backups/$backupId/download?token=${getToken()}"

  fun generateDownloadId(itemId: String, episodeId: String? = null): String =
    episodeId?.let { "$itemId|$it" } ?: itemId

  fun toReadableDate(long: Long, includeTime: Boolean = false): String {
    val instant = Instant.fromEpochMilliseconds(long)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val month =
      dateTime.month.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

    val datePart = "${dateTime.day} $month ${dateTime.year}"

    if (!includeTime) return datePart

    val hour12 =
      when {
        dateTime.hour == 0 -> 12
        dateTime.hour > 12 -> dateTime.hour - 12
        else -> dateTime.hour
      }

    val amPm = if (dateTime.hour < 12) "AM" else "PM"

    val timePart = "%d:%02d%s".format(hour12, dateTime.minute, amPm)

    return "$datePart $timePart"
  }

  /**
   * Converts an ISO-8601 timestamp string into a readable local date/time string.
   *
   * Examples:
   * - toReadableDate("2026-03-24T06:45:00Z", true) -> "24 March 2026 2:45PM"
   * - toReadableDate("2026-03-24T06:45:00Z") -> "24 March 2026"
   * - toReadableDate(null, true) -> null
   */
  fun toReadableDate(timestamp: String?, includeTime: Boolean = false): String? {
    if (timestamp.isNullOrBlank()) return null
    return runCatching {
        val instant = parse(timestamp)
        toReadableDate(instant.toEpochMilliseconds(), includeTime)
      }
      .getOrNull() ?: timestamp
  }

  /**
   * 65 -> 1 minute 5 seconds 86400 -> 1 day 90061 -> 1 day 1 hour 1 minute 1 second 31_536_000 -> 1
   * year 63_072_000 -> 2 years
   */
  fun formatDurationLong(seconds: Long): String {
    var remaining = seconds

    val years = remaining / 31_536_000
    remaining %= 31_536_000

    val days = remaining / 86_400
    remaining %= 86_400

    val hours = remaining / 3_600
    remaining %= 3_600

    val minutes = remaining / 60
    remaining %= 60

    val secs = remaining

    val parts =
      listOfNotNull(
        years.takeIf { it > 0 }?.let { "$it year${if (it > 1) "s" else ""}" },
        days.takeIf { it > 0 }?.let { "$it day${if (it > 1) "s" else ""}" },
        hours.takeIf { it > 0 }?.let { "$it hour${if (it > 1) "s" else ""}" },
        minutes.takeIf { it > 0 }?.let { "$it minute${if (it > 1) "s" else ""}" },
        secs.takeIf { it > 0 }?.let { "$it second${if (it > 1) "s" else ""}" },
      )

    return parts.joinToString(" ")
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

  fun toLocalDateTime(timestamp: String): LocalDateTime? {
    val format = LocalDateTime.Format {
      year()
      char('-')
      monthNumber()
      char('-')
      day()
      char(' ')
      hour()
      char(':')
      minute()
      char(':')
      second()
      char('.')
      secondFraction(3)
    }

    val result = runCatching { LocalDateTime.parse(timestamp, format) }.getOrNull()
    return result
  }

  /**
   * Convert kotlin LocalDateTime to 12 hour format
   * * Examples:
   * - 12:00 AM
   * - 03:00 PM
   */
  fun formatHour(dateTime: LocalDateTime): String {
    val hour = dateTime.hour
    val ampm = if (hour < 12) "AM" else "PM"
    val hour12 =
      when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
      }
    return "$hour12 $ampm"
  }

  fun formatFileSize(bytes: Long): String {
    return when {
      bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
      bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
      bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
      else -> "$bytes B"
    }
  }

  /**
   * Calculates the next occurrence of a cron schedule from now. Supports standard 5-field cron:
   * minute hour dom month dow
   * - "*" wildcards supported for dom, month
   * - Specific day-of-week (0=Sun, 1=Mon, ... 6=Sat) supported Returns a readable date string, or
   *   empty string if the expression is invalid.
   */
  fun nextCronDate(cronExpression: String): String {
    if (cronExpression.isBlank()) return ""
    val parts = cronExpression.trim().split("\\s+".toRegex())
    if (parts.size < 5) return ""
    val minute = parts[0].toIntOrNull() ?: return ""
    val hour = parts[1].toIntOrNull() ?: return ""
    val dowStr = parts[4]

    val tz = TimeZone.currentSystemDefault()
    val nowInstant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val now = nowInstant.toLocalDateTime(tz)

    // Start candidate at today's target time
    var candidate = LocalDateTime(now.year, now.month, now.day, hour, minute, 0, 0)

    // If that time is in the past, advance by 1 day
    if (
      !candidate.toInstant(tz).toEpochMilliseconds().let { it > nowInstant.toEpochMilliseconds() }
    ) {
      val nextDate = candidate.date.plus(1, DateTimeUnit.DAY)
      candidate = LocalDateTime(nextDate.year, nextDate.month, nextDate.day, hour, minute, 0, 0)
    }

    // If a specific day-of-week is required, advance until we hit it
    if (dowStr != "*") {
      val targetCronDow = dowStr.toIntOrNull() ?: return ""
      // Cron: 0=Sun..6=Sat  →  kotlinx ordinal: Mon=0..Sun=6
      // Formula: (cronDow + 6) % 7
      val targetOrdinal = (targetCronDow + 6) % 7
      var attempts = 0
      while (candidate.dayOfWeek.ordinal != targetOrdinal && attempts < 8) {
        val nextDate = candidate.date.plus(1, DateTimeUnit.DAY)
        candidate = LocalDateTime(nextDate.year, nextDate.month, nextDate.day, hour, minute, 0, 0)
        attempts++
      }
    }

    return toReadableDate(candidate.toInstant(tz).toEpochMilliseconds(), includeTime = true)
  }

  fun nowMilis(): Long {
    return System.currentTimeMillis()
  }

  fun createOpenPlayerIntent(mediaId: String, context: Context): PendingIntent =
    createGenericIntent(context, ACTION_OPEN_PLAYER, mediaId)

  fun createOpenDetailIntent(mediaId: String, context: Context): PendingIntent =
    createGenericIntent(context, ACTION_OPEN_DETAIL, mediaId)

  fun toReadablePlayMethod(method: Int): String {
    val result =
      when (method) {
        0 -> context.getString(R.string.direct_play)
        1 -> context.getString(R.string.direct_stream)
        2 -> context.getString(R.string.transcode)
        3 -> context.getString(R.string.local)
        else -> context.getString(R.string.unknown)
      }
    return result
  }

  fun progress(progress: Double): String {
    val progressRounded = (progress * 100).roundToInt()
    return "$progressRounded%"
  }

  fun getRelativeTimeAndroid(timestampMs: Long?): String {
    if (timestampMs == null) return ""
    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      )
      .toString()
  }

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
