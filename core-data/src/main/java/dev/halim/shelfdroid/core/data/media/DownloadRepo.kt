package dev.halim.shelfdroid.core.data.media

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("UnsafeOptInUsageError")
@Singleton
class DownloadRepo @Inject constructor(private val downloadManager: DownloadManager) {

  private val _downloads = MutableStateFlow(fetch())
  val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

  init {
    downloadManager.addListener(
      object : DownloadManager.Listener {
        override fun onIdle(downloadManager: DownloadManager) {
          updateDownloadsIfChanged()
        }

        override fun onDownloadChanged(
          downloadManager: DownloadManager,
          download: Download,
          finalException: Exception?,
        ) {
          updateDownloadInList(download)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
          removeDownloadFromList(download)
        }
      }
    )
  }

  private fun updateDownloadsIfChanged() {
    val newList = fetch()
    if (newList.size != _downloads.value.size) {
      _downloads.value = newList
    }
  }

  private fun updateDownloadInList(download: Download) {
    _downloads.update { current ->
      val index = current.indexOfFirst { it.request.id == download.request.id }
      if (index != -1) {
        current.toMutableList().apply { this[index] = download }
      } else {
        current + download
      }
    }
  }

  private fun removeDownloadFromList(download: Download) {
    _downloads.update { current -> current.filterNot { it.request.id == download.request.id } }
  }

  private fun fetch(): List<Download> {
    return runCatching {
        buildList {
          downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
              add(cursor.download)
            }
          }
        }
      }
      .getOrElse { emptyList() }
  }
}
