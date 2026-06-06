package dev.halim.shelfdroid.download

import androidx.media3.exoplayer.offline.Download
import org.json.JSONObject

data class DownloadNotificationPayload(
  val title: String,
  val openDetailId: String,
  val secondaryLabel: String? = null,
  val kind: String = KIND_SINGLE,
  val batchId: String? = null,
  val batchTitle: String? = null,
  val trackCount: Int? = null,
  val filename: String? = null,
  val relativePath: String? = null,
) {

  fun encode(): ByteArray {
    return JSONObject()
      .put(KEY_TITLE, title)
      .put(KEY_OPEN_DETAIL_ID, openDetailId)
      .put(KEY_SECONDARY_LABEL, secondaryLabel)
      .put(KEY_KIND, kind)
      .put(KEY_BATCH_ID, batchId)
      .put(KEY_BATCH_TITLE, batchTitle)
      .put(KEY_TRACK_COUNT, trackCount)
      .put(KEY_FILENAME, filename)
      .put(KEY_RELATIVE_PATH, relativePath)
      .toString()
      .toByteArray()
  }

  fun groupKey(downloadId: String): String = batchId ?: downloadId

  fun openDetailTargetId(downloadId: String): String =
    batchId ?: openDetailId.ifBlank { downloadId }

  fun displayTitle(): String = batchTitle ?: title

  fun terminalDisplayText(): String {
    val secondary = secondaryLabel?.takeIf { it.isNotBlank() } ?: return displayTitle()
    return "${displayTitle()} - $secondary"
  }

  fun isBookBatch(): Boolean = kind == KIND_BOOK_BATCH_TRACK && !batchId.isNullOrBlank()

  fun isBookTrack(): Boolean =
    kind == KIND_BOOK_BATCH_TRACK && !filename.isNullOrBlank() && !relativePath.isNullOrBlank()

  fun isPodcastEpisode(): Boolean = kind == KIND_PODCAST_EPISODE && !filename.isNullOrBlank()

  companion object {
    private const val KEY_TITLE = "title"
    private const val KEY_OPEN_DETAIL_ID = "openDetailId"
    private const val KEY_SECONDARY_LABEL = "secondaryLabel"
    private const val KEY_KIND = "kind"
    private const val KEY_BATCH_ID = "batchId"
    private const val KEY_BATCH_TITLE = "batchTitle"
    private const val KEY_TRACK_COUNT = "trackCount"
    private const val KEY_FILENAME = "filename"
    private const val KEY_RELATIVE_PATH = "relativePath"

    private const val KIND_SINGLE = "single"
    private const val KIND_BOOK_BATCH_TRACK = "book_batch_track"
    private const val KIND_PODCAST_EPISODE = "podcast_episode"

    fun single(
      title: String,
      openDetailId: String,
      secondaryLabel: String? = null,
    ): DownloadNotificationPayload {
      return DownloadNotificationPayload(
        title = title,
        openDetailId = openDetailId,
        secondaryLabel = secondaryLabel,
      )
    }

    fun bookBatchTrack(
      bookId: String,
      bookTitle: String,
      author: String? = null,
      trackCount: Int,
      filename: String,
      relativePath: String,
    ): DownloadNotificationPayload {
      return DownloadNotificationPayload(
        title = bookTitle,
        openDetailId = bookId,
        secondaryLabel = author,
        kind = KIND_BOOK_BATCH_TRACK,
        batchId = bookId,
        batchTitle = bookTitle,
        trackCount = trackCount,
        filename = filename,
        relativePath = relativePath,
      )
    }

    fun podcastEpisode(
      title: String,
      openDetailId: String,
      podcastTitle: String,
      filename: String,
    ): DownloadNotificationPayload {
      return DownloadNotificationPayload(
        title = title,
        openDetailId = openDetailId,
        secondaryLabel = podcastTitle,
        kind = KIND_PODCAST_EPISODE,
        filename = filename,
      )
    }

    fun fromDownload(download: Download): DownloadNotificationPayload {
      return fromBytes(download.request.data, download.request.id)
    }

    fun fromBytes(bytes: ByteArray, fallbackDownloadId: String): DownloadNotificationPayload {
      val raw = bytes.decodeToString()
      return runCatching {
          val json = JSONObject(raw)
          DownloadNotificationPayload(
            title = json.optString(KEY_TITLE, raw),
            openDetailId = json.optString(KEY_OPEN_DETAIL_ID, fallbackDownloadId),
            secondaryLabel = json.optString(KEY_SECONDARY_LABEL).ifBlank { null },
            kind = json.optString(KEY_KIND, KIND_SINGLE),
            batchId = json.optString(KEY_BATCH_ID).ifBlank { null },
            batchTitle = json.optString(KEY_BATCH_TITLE).ifBlank { null },
            trackCount = json.optInt(KEY_TRACK_COUNT).takeIf { it > 0 },
            filename = json.optString(KEY_FILENAME).ifBlank { null },
            relativePath = json.optString(KEY_RELATIVE_PATH).ifBlank { null },
          )
        }
        .getOrElse { single(title = raw, openDetailId = fallbackDownloadId) }
    }
  }
}
