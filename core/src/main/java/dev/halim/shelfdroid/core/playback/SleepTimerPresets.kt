package dev.halim.shelfdroid.core.playback

val SLEEP_TIMER_PRESET_MINUTES: List<Int> = listOf(1, 5, 10, 15, 30, 45, 60)

val DEFAULT_SLEEP_TIMER_CYCLE: List<Int> = listOf(1, 5)

fun normalizeSleepTimerCycle(minutes: List<Int>): List<Int> {
  val normalized = minutes.filter { it in SLEEP_TIMER_PRESET_MINUTES }.distinct().sorted()
  return normalized.ifEmpty { DEFAULT_SLEEP_TIMER_CYCLE }
}
