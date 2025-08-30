package dev.halim.shelfdroid.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.DownloadState
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@SuppressLint("UnsafeOptInUsageError")
@Singleton
class DownloadRepo
@Inject
constructor(
  private val downloadManager: DownloadManager,
  private val helper: Helper,
  private val downloadMapper: DownloadMapper,
  @ApplicationContext private val context: Context,
) {

  private val _downloads = MutableStateFlow(fetch())
  val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

  val completedDownloads =
    downloads.map { downloads -> downloads.filter { it.state == Download.STATE_COMPLETED } }

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

  fun fetchPodcast(id: String): List<Download> {
    return downloads.value.filter {
      it.request.id.substringBefore("|") == id && it.state == Download.STATE_COMPLETED
    }
  }

  fun isBookDownloaded(id: String, trackIndexes: List<Int>): Boolean {
    val downloadedIds =
      downloads.value.filter { it.state == Download.STATE_COMPLETED }.map { it.request.id }

    return when (trackIndexes.size) {
      1 -> id in downloadedIds
      0 -> false
      else -> {
        val ids =
          trackIndexes.map { trackIndex -> helper.generateDownloadId(id, trackIndex.toString()) }
        ids.all { id -> id in downloadedIds }
      }
    }
  }

  fun podcastDownloadedEpisodeIds(id: String): Set<String> {
    return downloads.value
      .asSequence()
      .filter { it.state == Download.STATE_COMPLETED }
      .map { it.request.id }
      .filter { it.contains(id) }
      .map { it.substringAfter("|") }
      .toSet()
  }

  suspend fun item(
    itemId: String,
    episodeId: String? = null,
    url: String,
    title: String,
  ): DownloadUiState {
    val downloadId = helper.generateDownloadId(itemId, episodeId)
    val download = downloadById(downloadId)
    val downloadState = downloadMapper.toDownloadState(download?.state)
    val downloadUrl = helper.generateContentUrl(url)

    return DownloadUiState(state = downloadState, id = downloadId, url = downloadUrl, title = title)
  }

  suspend fun multipleTrackItem(
    itemId: String,
    title: String,
    tracks: List<AudioTrack>,
  ): MultipleTrackDownloadUiState {
    val titleShort = if (title.length > 27) title.take(27) + "..." else title
    val size = tracks.size
    val items =
      tracks.map { track ->
        val downloadId = helper.generateDownloadId(itemId, track.index.toString())
        val download = downloadById(downloadId)
        val downloadState = downloadMapper.toDownloadState(download?.state)
        val downloadUrl = helper.generateContentUrl(track.contentUrl)
        DownloadUiState(
          state = downloadState,
          id = downloadId,
          url = downloadUrl,
          title = "$titleShort ${track.index}/$size",
        )
      }

    val state =
      when {
        items.all { it.state == DownloadState.Completed } -> DownloadState.Completed
        items.all { it.state == DownloadState.Failed } -> DownloadState.Failed
        items.any { it.state == DownloadState.Downloading } -> DownloadState.Downloading
        items.any { it.state == DownloadState.Failed } &&
          items.any { it.state == DownloadState.Completed } -> DownloadState.Incomplete
        else -> DownloadState.Unknown
      }
    return MultipleTrackDownloadUiState(state = state, items = items)
  }

  fun download(id: String, url: String, message: String) {
    DownloadService.sendAddDownload(
      context,
      ShelfDownloadService::class.java,
      toDownloadRequest(id, url, message),
      true,
    )
  }

  fun delete(id: String) {
    DownloadService.sendRemoveDownload(context, ShelfDownloadService::class.java, id, false)
  }

  fun cleanupBook(ids: List<String>) {
    val toDelete = downloads.value.map { it.request.id }.filter { it.substringBefore("|") in ids }

    toDelete.forEach { delete(it) }
  }

  fun cleanupEpisode(ids: List<String>) {
    val toDelete = downloads.value.map { it.request.id }.filter { it.substringAfter("|") in ids }

    toDelete.forEach { delete(it) }
  }

  private fun toDownloadRequest(id: String, url: String, message: String): DownloadRequest {
    return DownloadRequest.Builder(id, url.toUri())
      .setCustomCacheKey(id)
      .setData(message.toByteArray())
      .build()
  }

  private fun downloadById(id: String): Download? {
    return _downloads.value.find { it.request.id == id }
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
