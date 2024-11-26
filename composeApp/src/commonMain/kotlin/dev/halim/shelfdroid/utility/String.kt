package dev.halim.shelfdroid.utility

fun formatTime(inputInSeconds: Long): String {
    val hours = inputInSeconds / 3600
    val minutes = (inputInSeconds % 3600) / 60
    val seconds = inputInSeconds % 60

    return when {
        hours > 0 -> "${hours.padZero()}:${minutes.padZero()}:${seconds.padZero()}"
        else -> "${minutes.padZero()}:${seconds.padZero()}"
    }
}

fun Long.padZero(): String = if (this < 10) "0$this" else this.toString()