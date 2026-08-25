package dev.halim.shelfdroid.core.data.screen.libraryadministration

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Website-equivalent presets for an automatic Library scan schedule. */
enum class LibraryAdministrationScheduleInterval(val presetCron: String? = null) {
  Custom,
  Daily,
  Every12Hours("0 */12 * * *"),
  Every6Hours("0 */6 * * *"),
  Every2Hours("0 */2 * * *"),
  EveryHour("0 * * * *"),
  Every30Minutes("*/30 * * * *"),
  Every15Minutes("*/15 * * * *"),
}

enum class LibraryAdministrationScheduleMode {
  Simple,
  Advanced,
}

val ALL_LIBRARY_SCHEDULE_WEEKDAYS: Set<Int> = setOf(0, 1, 2, 3, 4, 5, 6)

/** Values used by the weekday/time portion of the Schedule tab. */
data class LibraryAdministrationSimpleSchedule(
  val interval: LibraryAdministrationScheduleInterval = LibraryAdministrationScheduleInterval.Custom,
  val hour: String = "0",
  val minute: String = "0",
  /** Cron weekday values: Sunday is 0, Monday is 1, through Saturday is 6. */
  val weekdays: Set<Int> = setOf(1),
) {
  fun toCronExpressionOrNull(): String? =
    when (interval) {
      LibraryAdministrationScheduleInterval.Custom -> {
        val minuteValue = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val hourValue = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val selectedWeekdays = weekdays.takeIf { it.isNotEmpty() }?.filter { it in 0..6 }?.sorted()
          ?: return null
        if (selectedWeekdays.size != weekdays.size) return null
        val dayPiece =
          if (selectedWeekdays.size == ALL_LIBRARY_SCHEDULE_WEEKDAYS.size) {
            "*"
          } else {
            selectedWeekdays.joinToString(",")
          }
        "$minuteValue $hourValue * * $dayPiece"
      }
      LibraryAdministrationScheduleInterval.Daily -> {
        val minuteValue = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val hourValue = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        "$minuteValue $hourValue * * *"
      }
      else -> interval.presetCron
    }

  fun validationMessage(): String? =
    when (interval) {
      LibraryAdministrationScheduleInterval.Custom -> {
        when {
          hour.toIntOrNull() !in 0..23 -> "Hour must be between 0 and 23."
          minute.toIntOrNull() !in 0..59 -> "Minute must be between 0 and 59."
          weekdays.isEmpty() -> "Select at least one weekday."
          else -> null
        }
      }
      LibraryAdministrationScheduleInterval.Daily ->
        when {
          hour.toIntOrNull() !in 0..23 -> "Hour must be between 0 and 23."
          minute.toIntOrNull() !in 0..59 -> "Minute must be between 0 and 59."
          else -> null
        }
      else -> null
    }

  fun summary(): String? {
    val expression = toCronExpressionOrNull() ?: return null
    return when (interval) {
      LibraryAdministrationScheduleInterval.Custom -> {
        val days =
          if (weekdays.size == ALL_LIBRARY_SCHEDULE_WEEKDAYS.size) {
            "day"
          } else {
            weekdays.sorted().joinToString(", ") { LIBRARY_SCHEDULE_WEEKDAY_NAMES.getValue(it) }
          }
        "Run every $days at ${formatScheduleTime(expression)}"
      }
      LibraryAdministrationScheduleInterval.Daily ->
        "Run every day at ${formatScheduleTime(expression)}"
      LibraryAdministrationScheduleInterval.Every12Hours -> "Run every 12 hours"
      LibraryAdministrationScheduleInterval.Every6Hours -> "Run every 6 hours"
      LibraryAdministrationScheduleInterval.Every2Hours -> "Run every 2 hours"
      LibraryAdministrationScheduleInterval.EveryHour -> "Run every hour"
      LibraryAdministrationScheduleInterval.Every30Minutes -> "Run every 30 minutes"
      LibraryAdministrationScheduleInterval.Every15Minutes -> "Run every 15 minutes"
    }
  }
}

/** Draft state for the reusable create/edit Schedule section. */
data class LibraryAdministrationScheduleDraft(
  val enabled: Boolean = false,
  val mode: LibraryAdministrationScheduleMode = LibraryAdministrationScheduleMode.Simple,
  val simple: LibraryAdministrationSimpleSchedule = LibraryAdministrationSimpleSchedule(),
  /** Kept independently so switching modes never loses an intentional advanced draft. */
  val advancedCronExpression: String = "",
) {
  val cronExpression: String?
    get() =
      if (!enabled) null
      else {
        when (mode) {
          LibraryAdministrationScheduleMode.Simple -> simple.toCronExpressionOrNull()
          LibraryAdministrationScheduleMode.Advanced -> advancedCronExpression.trim().ifBlank { null }
        }
      }

  val summary: String?
    get() =
      when {
        !enabled -> null
        mode == LibraryAdministrationScheduleMode.Simple -> simple.summary()
        else -> cronExpression?.let { "Cron: $it" }
      }

  fun localValidationMessage(): String? =
    when {
      !enabled -> null
      mode == LibraryAdministrationScheduleMode.Simple ->
        simple.validationMessage()
          ?: if (simple.toCronExpressionOrNull() == null) "Choose a valid scan schedule."
          else null
      advancedCronExpression.trim().split(WHITESPACE_REGEX).size != 5 ->
        "Enter a five-field cron expression."
      else -> null
    }
}

/** Full draft schedule used by create serialization. Disabled schedules submit no cron field. */
fun LibraryAdministrationDraft.scheduleExpressionOrNull(): String? = schedule.cronExpression

/** Returns a readable next occurrence for the standard five-field cron syntax. */
fun nextLibraryScheduleRun(expression: String, now: ZonedDateTime = ZonedDateTime.now()): String {
  val fields = expression.trim().split(WHITESPACE_REGEX)
  if (fields.size != 5) return ""
  val start = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
  val limit = start.plusDays(366)
  var candidate = start
  while (candidate.isBefore(limit)) {
    val minuteMatches = matchesCronField(fields[0], candidate.minute, 0, 59)
    val hourMatches = matchesCronField(fields[1], candidate.hour, 0, 23)
    val dayOfMonthMatches = matchesCronField(fields[2], candidate.dayOfMonth, 1, 31)
    val monthMatches = matchesCronField(fields[3], candidate.monthValue, 1, 12)
    val weekday = candidate.dayOfWeek.toCronDay()
    val weekdayMatches = matchesCronField(fields[4], weekday, 0, 6)
    val dayOfMonthRestricted = fields[2] != "*"
    val dayOfWeekRestricted = fields[4] != "*"
    val dayMatches =
      if (dayOfMonthRestricted && dayOfWeekRestricted) dayOfMonthMatches || weekdayMatches
      else dayOfMonthMatches && weekdayMatches
    if (minuteMatches && hourMatches && monthMatches && dayMatches) {
      return candidate.format(NEXT_RUN_FORMATTER)
    }
    candidate = candidate.plusMinutes(1)
  }
  return ""
}

private fun DayOfWeek.toCronDay(): Int = value % 7

private fun matchesCronField(field: String, value: Int, minimum: Int, maximum: Int): Boolean {
  if (field == "*") return true
  return field.split(',').any { token ->
    val parts = token.split('/', limit = 2)
    val range = parts[0]
    val step = parts.getOrNull(1)?.toIntOrNull() ?: 1
    if (step <= 0) return@any false
    val (start, end) =
      when {
        range == "*" -> minimum to maximum
        range.contains('-') -> {
          val values = range.split('-', limit = 2).mapNotNull(String::toIntOrNull)
          if (values.size != 2) return@any false
          values[0] to values[1]
        }
        else -> {
          val single = range.toIntOrNull() ?: return@any false
          single to single
        }
      }
    if (start !in minimum..maximum || end !in minimum..maximum || start > end) return@any false
    value in start..end && (value - start) % step == 0
  }
}

private fun formatScheduleTime(expression: String): String {
  val fields = expression.split(' ')
  val minute = fields[0].toIntOrNull() ?: 0
  val hour = fields[1].toIntOrNull() ?: 0
  return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private val LIBRARY_SCHEDULE_WEEKDAY_NAMES =
  mapOf(
    0 to "Sunday",
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
  )

private val WHITESPACE_REGEX = "\\s+".toRegex()
private val NEXT_RUN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
