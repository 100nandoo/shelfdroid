package dev.halim.shelfdroid.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.DownloadState
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

data class BookCleanupRequest(
  val itemId: String,
  val title: String,
  val author: String?,
  val filenames: List<String>,
)

@SuppressLint("UnsafeOptInUsageError")
@Singleton
class DownloadRepo
@Inject
constructor(
  private val downloadManager: DownloadManager,
  private val helper: Helper,
  private val downloadMapper: DownloadMapper,
  private val bookDurableDownloadCatalog: BookDurableDownloadCatalog,
  private val podcastDurableDownloadCatalog: PodcastDurableDownloadCatalog,
  private val playerState: PlayerInternalStateHolder,
  @ApplicationContext private val context: Context,
) {

  private val _downloads = MutableStateFlow(fetch())
  val downloads = _downloads.asStateFlow()
  val durableDownloads: Flow<Int> =
    combine(bookDurableDownloadCatalog.changes, podcastDurableDownloadCatalog.changes) {
      bookChanges,
      podcastChanges ->
      bookChanges + podcastChanges
    }

  val completedDownloads = downloads.map { downloads ->
    downloads.filter { it.state == Download.STATE_COMPLETED }
  }

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

  fun isBookDownloaded(
    title: String,
    author: String?,
    tracks: List<AudioTrack>,
  ): Boolean {
    if (tracks.isEmpty()) return false

    val localTrackUris =
      bookDurableDownloadCatalog.trackUris(title, author, tracks.map { it.metadata.filename })
    return tracks.all { track -> track.metadata.filename in localTrackUris }
  }

  fun podcastDownloadedEpisodeIds(
    podcastTitle: String,
    episodes: List<PodcastEpisode>,
  ): Set<String> {
    return episodes
      .asSequence()
      .filter { episode ->
        isPodcastEpisodeDownloaded(
          podcastTitle = podcastTitle,
          filename = episode.audioTrack.metadata.filename,
        )
      }
      .map { it.id }
      .toSet()
  }

  suspend fun item(
    itemId: String,
    episodeId: String? = null,
    url: String,
    title: String,
    secondaryLabel: String = "",
    filename: String = "",
  ): DownloadUiState {
    val downloadId = helper.generateDownloadId(itemId, episodeId)
    val download = downloadById(downloadId)
    val durableEpisodeUri =
      if (episodeId != null && secondaryLabel.isNotBlank() && filename.isNotBlank()) {
        podcastDurableDownloadCatalog.findEpisodeUri(secondaryLabel, filename)
      } else {
        null
      }
    val downloadState =
      if (durableEpisodeUri != null) {
        DownloadState.Completed
      } else if (episodeId != null && download?.state == Download.STATE_COMPLETED) {
        DownloadState.Unknown
      } else {
        downloadMapper.toDownloadState(download?.state)
      }
    val downloadUrl = helper.generateContentUrl(url)

    return DownloadUiState(
      state = downloadState,
      id = downloadId,
      url = downloadUrl,
      title = title,
      secondaryLabel = secondaryLabel,
      filename = filename,
    )
  }

  suspend fun bookItem(
    itemId: String,
    bookTitle: String,
    author: String?,
    track: AudioTrack,
  ): DownloadUiState {
    val downloadId = itemId
    val download = downloadById(downloadId)
    val localTrackUri =
      bookDurableDownloadCatalog.trackUris(bookTitle, author, listOf(track.metadata.filename))[
        track.metadata.filename
      ]
    val downloadState = trackDownloadState(download, localTrackUri != null)
    val downloadUrl = helper.generateContentUrl(track.contentUrl)

    return DownloadUiState(
      state = downloadState,
      id = downloadId,
      url = downloadUrl,
      title = bookTitle,
      secondaryLabel = author.orEmpty(),
      filename = track.metadata.filename,
    )
  }

  suspend fun multipleTrackItem(
    itemId: String,
    title: String,
    author: String?,
    tracks: List<AudioTrack>,
  ): MultipleTrackDownloadUiState {
    val titleShort = if (title.length > 27) title.take(27) + "..." else title
    val size = tracks.size
    val localTrackUris =
      bookDurableDownloadCatalog.trackUris(title, author, tracks.map { it.metadata.filename })
    val items = tracks.map { track ->
      val downloadId = helper.generateDownloadId(itemId, track.index.toString())
      val download = downloadById(downloadId)
      val downloadState =
        trackDownloadState(download, track.metadata.filename in localTrackUris)
      val downloadUrl = helper.generateContentUrl(track.contentUrl)
      DownloadUiState(
        state = downloadState,
        id = downloadId,
        url = downloadUrl,
        title = "$titleShort ${track.index}/$size",
        secondaryLabel = author.orEmpty(),
        filename = track.metadata.filename,
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

  fun download(id: String, url: String, message: String, secondaryLabel: String? = null) {
    val payload =
      DownloadNotificationPayload.single(
        title = message,
        openDetailId = id,
        secondaryLabel = secondaryLabel,
      )
    enqueueDownload(id = id, url = url, payload = payload, foreground = true)
  }

  fun downloadPodcastEpisode(download: DownloadUiState) {
    val podcastTitle = download.secondaryLabel
    val filename = download.filename
    if (podcastTitle.isBlank() || filename.isBlank()) {
      download(download.id, download.url, download.title, download.secondaryLabel)
      return
    }

    if (
      isActivePodcastEpisode(download.id) && localPodcastEpisodeUri(podcastTitle, filename) != null
    ) {
      return
    }

    podcastDurableDownloadCatalog.deleteEpisode(podcastTitle, filename)
    if (downloadById(download.id) != null) {
      downloadManager.removeDownload(download.id)
    }

    val payload =
      DownloadNotificationPayload.podcastEpisode(
        title = download.title,
        openDetailId = download.id,
        podcastTitle = podcastTitle,
        filename = filename,
      )
    enqueueDownload(id = download.id, url = download.url, payload = payload, foreground = true)
  }

  fun downloadBook(
    itemId: String,
    title: String,
    author: String? = null,
    tracks: List<DownloadUiState>,
  ) {
    if (tracks.isEmpty()) return

    val filenames = tracks.map { it.filename }.filter { it.isNotBlank() }
    val relativePath = bookDurableDownloadCatalog.resolveRelativePath(title, author, filenames)

    if (isActiveBook(itemId) && localBookTrackUrisByFilename(title, author, filenames).isNotEmpty()) {
      return
    }

    bookDurableDownloadCatalog.deleteBookFolderContents(relativePath)
    tracks.forEach { track ->
      if (downloadById(track.id) != null) {
        downloadManager.removeDownload(track.id)
      }
    }

    tracks.forEachIndexed { index, track ->
      val payload =
        DownloadNotificationPayload.bookBatchTrack(
          bookId = itemId,
          bookTitle = title,
          author = author,
          trackCount = tracks.size,
          filename = track.filename,
          relativePath = relativePath,
        )
      enqueueDownload(id = track.id, url = track.url, payload = payload, foreground = index == 0)
    }
  }

  private fun enqueueDownload(
    id: String,
    url: String,
    payload: DownloadNotificationPayload,
    foreground: Boolean,
  ) {
    DownloadService.sendAddDownload(
      context,
      ShelfDownloadService::class.java,
      toDownloadRequest(id, url, payload),
      foreground,
    )
  }

  fun delete(id: String) {
    DownloadService.sendRemoveDownload(context, ShelfDownloadService::class.java, id, false)
  }

  fun deletePodcastEpisode(download: DownloadUiState) {
    if (download.secondaryLabel.isNotBlank() && download.filename.isNotBlank()) {
      podcastDurableDownloadCatalog.deleteEpisode(download.secondaryLabel, download.filename)
    }
    delete(download.id)
  }

  fun deleteBook(
    title: String,
    author: String?,
    tracks: List<DownloadUiState>,
  ) {
    if (tracks.isEmpty()) return

    val filenames = tracks.map { it.filename }.filter { it.isNotBlank() }
    bookDurableDownloadCatalog.deleteBook(title, author, filenames)
    tracks.forEach { delete(it.id) }
  }

  fun cleanupBooks(books: List<BookCleanupRequest>) {
    books.forEach { book ->
      bookDurableDownloadCatalog.deleteBook(book.title, book.author, book.filenames)
      val toDelete =
        downloads.value
          .map { it.request.id }
          .filter { it == book.itemId || it.substringBefore("|") == book.itemId }
      toDelete.forEach { delete(it) }
    }
  }

  fun cleanupEpisode(ids: List<String>) {
    val toDelete = downloads.value.map { it.request.id }.filter { it.substringAfter("|") in ids }

    toDelete.forEach { delete(it) }
  }

  private fun toDownloadRequest(
    id: String,
    url: String,
    payload: DownloadNotificationPayload,
  ): DownloadRequest {
    return DownloadRequest.Builder(id, url.toUri())
      .setCustomCacheKey(id)
      .setData(payload.encode())
      .build()
  }

  private fun downloadById(id: String): Download? {
    return _downloads.value.find { it.request.id == id }
  }

  fun localPodcastEpisodeUri(podcastTitle: String, filename: String): String? {
    return podcastDurableDownloadCatalog.findEpisodeUri(podcastTitle, filename)?.toString()
  }

  fun localBookTrackUris(
    title: String,
    author: String?,
    tracks: List<AudioTrack>,
  ): Map<Int, String> {
    val localTrackUris =
      bookDurableDownloadCatalog.trackUris(title, author, tracks.map { it.metadata.filename })
    return tracks.mapNotNull { track ->
      localTrackUris[track.metadata.filename]?.toString()?.let { uri -> track.index to uri }
    }.toMap()
  }

  private fun localBookTrackUrisByFilename(
    title: String,
    author: String?,
    filenames: List<String>,
  ): Map<String, String> {
    return bookDurableDownloadCatalog
      .trackUris(title, author, filenames)
      .mapValues { (_, uri) -> uri.toString() }
  }

  private fun isPodcastEpisodeDownloaded(
    podcastTitle: String,
    filename: String,
  ): Boolean {
    return podcastDurableDownloadCatalog.findEpisodeUri(podcastTitle, filename) != null
  }

  private fun trackDownloadState(download: Download?, durableUriExists: Boolean): DownloadState {
    return if (durableUriExists) {
      DownloadState.Completed
    } else if (download?.state == Download.STATE_COMPLETED) {
      DownloadState.Unknown
    } else {
      downloadMapper.toDownloadState(download?.state)
    }
  }

  private fun isActiveBook(itemId: String): Boolean {
    if (!playerState.isBook() || !playerState.isPlaying()) return false
    return playerState.itemId() == itemId
  }

  private fun isActivePodcastEpisode(downloadId: String): Boolean {
    if (playerState.isBook() || !playerState.isPlaying()) return false
    return helper.generateDownloadId(playerState.itemId(), playerState.episodeId()) == downloadId
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
