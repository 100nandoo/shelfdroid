package dev.halim.shelfdroid.utility

import kotlin.math.ceil
import kotlin.time.Duration

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