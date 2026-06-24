package dev.halim.shelfdroid.core.data.screen.edititem

val ALL_WEEKDAYS: Set<Int> = setOf(0, 1, 2, 3, 4, 5, 6)

enum class PodcastScheduleMode {
  Simple,
  Advanced,
}

enum class PodcastScheduleSimpleInterval(val presetCron: String? = null) {
  Custom,
  Daily,
  Every12Hours("0 */12 * * *"),
  Every6Hours("0 */6 * * *"),
  Every2Hours("0 */2 * * *"),
  EveryHour("0 * * * *"),
  Every30Minutes("*/30 * * * *"),
  Every15Minutes("*/15 * * * *"),
}

data class PodcastScheduleSimpleBuilder(
  val interval: PodcastScheduleSimpleInterval = PodcastScheduleSimpleInterval.Daily,
  val selectedHour: String = "0",
  val selectedMinute: String = "0",
  val selectedWeekdays: Set<Int> = ALL_WEEKDAYS,
)

data class PodcastSchedulePresentation(
  val mode: PodcastScheduleMode,
  val simpleBuilder: PodcastScheduleSimpleBuilder,
)

fun defaultPodcastScheduleSimpleBuilder(): PodcastScheduleSimpleBuilder =
  PodcastScheduleSimpleBuilder()

fun deriveSchedulePresentation(
  schedule: PodcastScheduleForm,
  preferredMode: PodcastScheduleMode? = null,
  currentBuilder: PodcastScheduleSimpleBuilder? = null,
): PodcastSchedulePresentation {
  val parsedBuilder = parseSimpleSchedule(schedule.cronExpression)
  return PodcastSchedulePresentation(
    mode =
      preferredMode
        ?: if (parsedBuilder != null) PodcastScheduleMode.Simple else PodcastScheduleMode.Advanced,
    simpleBuilder = parsedBuilder ?: currentBuilder ?: defaultPodcastScheduleSimpleBuilder(),
  )
}

fun parseSimpleSchedule(cronExpression: String): PodcastScheduleSimpleBuilder? {
  val normalizedCron = cronExpression.trim()
  if (normalizedCron.isBlank()) return null

  PodcastScheduleSimpleInterval.entries
    .firstOrNull { it.presetCron == normalizedCron }
    ?.let {
      return PodcastScheduleSimpleBuilder(interval = it)
    }

  val pieces = normalizedCron.split(WHITESPACE_REGEX)
  if (pieces.size != 5) return null

  val minute = pieces[0].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
  val hour = pieces[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
  if (pieces[2] != "*" || pieces[3] != "*") return null

  return when (val weekdays = parseWeekdays(pieces[4])) {
    null -> null
    ALL_WEEKDAYS ->
      PodcastScheduleSimpleBuilder(
        interval = PodcastScheduleSimpleInterval.Daily,
        selectedHour = hour.toString(),
        selectedMinute = minute.toString(),
        selectedWeekdays = weekdays,
      )
    else ->
      PodcastScheduleSimpleBuilder(
        interval = PodcastScheduleSimpleInterval.Custom,
        selectedHour = hour.toString(),
        selectedMinute = minute.toString(),
        selectedWeekdays = weekdays,
      )
  }
}

fun PodcastScheduleSimpleBuilder.toCronExpressionOrNull(): String? =
  when (interval) {
    PodcastScheduleSimpleInterval.Custom -> {
      val minute = selectedMinute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
      val hour = selectedHour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
      val weekdays = selectedWeekdays.takeIf { it.isNotEmpty() }?.sorted() ?: return null
      val dayPiece = if (weekdays.size == ALL_WEEKDAYS.size) "*" else weekdays.joinToString(",")
      "$minute $hour * * $dayPiece"
    }

    PodcastScheduleSimpleInterval.Daily -> {
      val minute = selectedMinute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
      val hour = selectedHour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
      "$minute $hour * * *"
    }

    else -> interval.presetCron
  }

fun PodcastScheduleSimpleBuilder.summary(): String? =
  when (interval) {
    PodcastScheduleSimpleInterval.Custom -> {
      val cron = toCronExpressionOrNull() ?: return null
      val days =
        if (selectedWeekdays.size == ALL_WEEKDAYS.size) {
          "day"
        } else {
          selectedWeekdays.sorted().joinToString(", ") { WEEKDAY_NAMES.getValue(it) }
        }
      "Run every $days at ${formatTimeForSummary(cron)}"
    }

    PodcastScheduleSimpleInterval.Daily -> {
      val cron = toCronExpressionOrNull() ?: return null
      "Run every day at ${formatTimeForSummary(cron)}"
    }

    PodcastScheduleSimpleInterval.Every12Hours -> "Run every 12 hours"
    PodcastScheduleSimpleInterval.Every6Hours -> "Run every 6 hours"
    PodcastScheduleSimpleInterval.Every2Hours -> "Run every 2 hours"
    PodcastScheduleSimpleInterval.EveryHour -> "Run every hour"
    PodcastScheduleSimpleInterval.Every30Minutes -> "Run every 30 minutes"
    PodcastScheduleSimpleInterval.Every15Minutes -> "Run every 15 minutes"
  }

fun EditItemUiState.simpleBuilderOwnsScheduleCron(): Boolean =
  simpleScheduleBuilder.toCronExpressionOrNull()?.trim() == schedule.cronExpression.trim()

fun EditItemUiState.shouldValidateScheduleCron(): Boolean =
  schedule.autoDownloadEpisodes &&
    (scheduleMode == PodcastScheduleMode.Advanced || !simpleBuilderOwnsScheduleCron())

fun EditItemUiState.simpleScheduleGuidance(): String? =
  when {
    scheduleMode != PodcastScheduleMode.Simple || !schedule.autoDownloadEpisodes -> null
    simpleScheduleBuilder.validationMessage() != null -> simpleScheduleBuilder.validationMessage()
    simpleBuilderOwnsScheduleCron() -> simpleScheduleBuilder.summary()
    else -> SIMPLE_BUILDER_REPLACEMENT_HINT
  }

fun PodcastScheduleSimpleBuilder.validationMessage(): String? =
  when (interval) {
    PodcastScheduleSimpleInterval.Custom -> validateTimedBuilder(requireWeekdays = true)
    PodcastScheduleSimpleInterval.Daily -> validateTimedBuilder(requireWeekdays = false)
    else -> null
  }

private fun parseWeekdays(piece: String): Set<Int>? {
  if (piece == "*") return ALL_WEEKDAYS
  val weekdays =
    piece.split(",").mapNotNull { token ->
      token.toIntOrNull()?.let { if (it == 7) 0 else it }?.takeIf { it in 0..6 }
    }
  if (weekdays.isEmpty() || weekdays.size != piece.split(",").size) return null
  return weekdays.toSet()
}

private fun formatTimeForSummary(cronExpression: String): String {
  val pieces = cronExpression.split(' ')
  val minute = pieces[0].toIntOrNull() ?: 0
  val hour = pieces[1].toIntOrNull() ?: 0
  return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun PodcastScheduleSimpleBuilder.validateTimedBuilder(requireWeekdays: Boolean): String? {
  if (selectedHour.toIntOrNull() !in 0..23) return INVALID_HOUR_MESSAGE
  if (selectedMinute.toIntOrNull() !in 0..59) return INVALID_MINUTE_MESSAGE
  if (requireWeekdays && selectedWeekdays.isEmpty()) return SELECT_WEEKDAY_MESSAGE
  return null
}

private val WEEKDAY_NAMES =
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
private const val SIMPLE_BUILDER_REPLACEMENT_HINT =
  "Choose a simple schedule to replace the current advanced cron expression."
private const val INVALID_HOUR_MESSAGE = "Hour must be between 0 and 23."
private const val INVALID_MINUTE_MESSAGE = "Minute must be between 0 and 59."
private const val SELECT_WEEKDAY_MESSAGE = "Select at least one weekday."
