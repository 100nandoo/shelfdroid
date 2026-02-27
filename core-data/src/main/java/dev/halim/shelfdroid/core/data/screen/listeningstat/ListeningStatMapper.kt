@file:OptIn(ExperimentalTime::class)

package dev.halim.shelfdroid.core.data.screen.listeningstat

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.database.ListeningStatEntity
import dev.halim.shelfdroid.helper.Helper
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ListeningStatMapper @Inject constructor(private val helper: Helper) {
  fun toUiState(entity: ListeningStatEntity): ListeningStatUiState {
    val totalTime = helper.formatDurationLong(entity.totalTime)
    val today = helper.formatDurationLong(entity.today)
    return ListeningStatUiState(
      state = GenericState.Success,
      totalTime = totalTime,
      today = today,
      days = entity.days ?: emptyMap(),
      dayOfWeek = entity.dayOfWeek ?: emptyMap(),
      total = total(entity),
      thisWeek = thisWeek(entity),
    )
  }

  private fun total(entity: ListeningStatEntity): ListeningStatUiState.Total {
    val minutes = (entity.totalTime / 60)
    val string = String.format(Locale.getDefault(), "%,d", minutes)
    return ListeningStatUiState.Total(days = entity.days?.count().toString(), minutes = string)
  }

  private fun thisWeek(entity: ListeningStatEntity): ListeningStatUiState.ThisWeek {
    val (thisWeek, lastWeek) = splitWeeks(entity.days)

    val days = thisWeek.count()
    val lastWeekDays = lastWeek.count()
    val daysDelta = days - lastWeekDays

    val minutes = thisWeek.values.sum() / 60.0
    val lastWeekMinutes = lastWeek.values.sum() / 60.0
    val minutesDelta = ((minutes - lastWeekMinutes) / lastWeekMinutes * 100).roundToInt().toFloat()

    val mostMinutes = thisWeek.maxBy { it.value }.value / 60

    val streak = calculateStreak(thisWeek)
    val lastStreak = calculateBestStreak(lastWeek)
    val streakDelta = streak - lastStreak

    val dailyAverage = minutes / days
    val lastWeekdailyAverage = lastWeekMinutes / lastWeekDays
    val dailyAverageDelta =
      ((dailyAverage - lastWeekdailyAverage) / lastWeekdailyAverage * 100).roundToInt().toFloat()

    return ListeningStatUiState.ThisWeek(
      days = days.toString(),
      daysDelta = daysDelta,
      minutes = minutes.roundToInt().toString(),
      minutesDelta = minutesDelta,
      mostMinutes = mostMinutes.toString(),
      streak = streak.toString(),
      streakDelta = streakDelta,
      dailyAverage = dailyAverage.roundToInt().toString(),
      dailyAverageDelta = dailyAverageDelta,
    )
  }

  private fun splitWeeks(days: Map<String, Int>?): Pair<Map<String, Int>, Map<String, Int>> {

    if (days == null) return emptyMap<String, Int>() to emptyMap()

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val daysSinceMonday = today.dayOfWeek.ordinal

    // Monday = start of week (ISO standard)
    val startOfThisWeek = today.minus(daysSinceMonday, DateTimeUnit.DAY)
    val startOfLastWeek = startOfThisWeek.minus(7, DateTimeUnit.DAY)
    val endOfLastWeek = startOfThisWeek.minus(1, DateTimeUnit.DAY)

    val thisWeek = mutableMapOf<String, Int>()
    val lastWeek = mutableMapOf<String, Int>()

    days.forEach { (dateStr, value) ->
      val date = LocalDate.parse(dateStr)

      when {
        date >= startOfThisWeek -> {
          thisWeek[dateStr] = value
        }

        date in startOfLastWeek..endOfLastWeek -> {
          lastWeek[dateStr] = value
        }
      }
    }

    return thisWeek to lastWeek
  }

  fun calculateStreak(days: Map<String, Int>?): Int {
    if (days.isNullOrEmpty()) return 0

    // Convert all date strings to LocalDate
    val dates = days.keys.map { LocalDate.parse(it) }.toSet()

    // Start from today
    var streak = 0
    var date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Count consecutive days backwards
    while (dates.contains(date)) {
      streak++
      date = date.minus(1, DateTimeUnit.DAY)
    }

    return streak
  }

  fun calculateBestStreak(days: Map<String, Int>?): Int {
    if (days.isNullOrEmpty()) return 0

    // Convert all date strings to LocalDate and sort
    val sortedDates = days.keys.map { LocalDate.parse(it) }.sorted()

    var bestStreak = 0
    var currentStreak = 0
    var previousDate: LocalDate? = null

    for (date in sortedDates) {
      if (previousDate == null || date == previousDate.plus(1, DateTimeUnit.DAY)) {
        // consecutive day
        currentStreak++
      } else {
        // gap found, reset streak
        currentStreak = 1
      }

      if (currentStreak > bestStreak) bestStreak = currentStreak

      previousDate = date
    }

    return bestStreak
  }
}
