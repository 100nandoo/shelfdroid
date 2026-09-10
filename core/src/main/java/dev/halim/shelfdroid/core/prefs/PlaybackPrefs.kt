package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackPrefs(
  val keepSpeed: Boolean = false,
  val keepSleepTimer: Boolean = false,
  val episodeKeepSpeed: Boolean = true,
  val episodeKeepSleepTimer: Boolean = true,
  val bookKeepSpeed: Boolean = true,
  val bookKeepSleepTimer: Boolean = true,
)
