package dev.halim.shelfdroid.core.data.sessionreset

interface CurrentPlaybackCleanup {
  suspend fun clearCurrentPlayback()
}
