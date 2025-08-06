package dev.halim.shelfdroid.core.data.media

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import dev.halim.shelfdroid.core.DownloadState
import javax.inject.Inject

class DownloadMapper @Inject constructor() {
  @SuppressLint("UnsafeOptInUsageError")
  fun toDownloadState(state: Int?): DownloadState {
    return when (state) {
      Download.STATE_COMPLETED -> DownloadState.Completed
      Download.STATE_DOWNLOADING -> DownloadState.Downloading
      Download.STATE_QUEUED -> DownloadState.Queued
      Download.STATE_FAILED -> DownloadState.Failed
      else -> DownloadState.Unknown
    }
  }
}
