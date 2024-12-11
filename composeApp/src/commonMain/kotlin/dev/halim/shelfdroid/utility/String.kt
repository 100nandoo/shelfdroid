package dev.halim.shelfdroid.utility

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import kotlin.math.ceil
import kotlin.time.Duration
import kotlinx.datetime.*

fun formatTime(inputInSeconds: Long, padHour: Boolean = false): String {
    val hours = inputInSeconds / 3600
    val minutes = (inputInSeconds % 3600) / 60
    val seconds = inputInSeconds % 60

    return when {
        padHour || hours > 0 -> "${hours.padZero()}:${minutes.padZero()}:${seconds.padZero()}"
        else -> "${minutes.padZero()}:${seconds.padZero()}"
    }}

fun Long.padZero(): String = if (this < 10) "0$this" else this.toString()

fun sleepTimerMinute(duration: Duration): String {
    return ceil(duration.inWholeSeconds / 60.0).toInt().toString() + "m"
}

fun Long.toReadableDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
    return "${dateTime.dayOfMonth} ${dateTime.month.name.lowercase().capitalize(Locale.current)} ${dateTime.year}"
}

fun Float.toPercent(): String {
    return "${(this * 100).toInt()}%"
}