package dev.halim.shelfdroid.core.data.screen.settingsplayback

data class SettingsPlaybackUiState(
  val keepSpeed: Boolean = false,
  val keepSleepTimer: Boolean = false,
  val episodeKeepSpeed: Boolean = true,
  val episodeKeepSleepTimer: Boolean = true,
)
