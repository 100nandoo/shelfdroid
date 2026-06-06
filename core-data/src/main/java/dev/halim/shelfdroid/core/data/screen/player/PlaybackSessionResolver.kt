package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.DownloadState
import java.util.UUID
import javax.inject.Inject

class PlaybackSessionResolver @Inject constructor() {
  suspend fun resolve(
    downloadState: DownloadState,
    remoteSession: suspend () -> String,
  ): String {
    return if (downloadState.isDownloaded()) {
      UUID.randomUUID().toString()
    } else {
      remoteSession()
    }
  }
}
