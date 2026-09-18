package dev.halim.shelfdroid.core.playback

val PLAYBACK_SPEED_PRESET_VALUES: List<Float> = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

val DEFAULT_PLAYBACK_SPEED_CYCLE: List<Float> = listOf(1f, 1.25f, 1.5f, 2f)

fun normalizePlaybackSpeedCycle(speeds: List<Float>): List<Float> {
  val normalized = speeds.filter { it in PLAYBACK_SPEED_PRESET_VALUES }.distinct().sorted()
  return if (normalized.size >= 2) normalized else DEFAULT_PLAYBACK_SPEED_CYCLE
}

fun nextPlaybackSpeed(currentSpeed: Float, selectedSpeeds: List<Float>): Float {
  val speeds = normalizePlaybackSpeedCycle(selectedSpeeds)
  return speeds.firstOrNull { it > currentSpeed + 0.001f } ?: speeds.first()
}

fun normalizePlaybackSpeedToggleTarget(speed: Float): Float =
  speed.takeIf { it in PLAYBACK_SPEED_PRESET_VALUES && it != 1f } ?: 1.5f

fun togglePlaybackSpeed(currentSpeed: Float, targetSpeed: Float): Float {
  val target = normalizePlaybackSpeedToggleTarget(targetSpeed)
  return if (kotlin.math.abs(currentSpeed - target) <= 0.001f) 1f else target
}
