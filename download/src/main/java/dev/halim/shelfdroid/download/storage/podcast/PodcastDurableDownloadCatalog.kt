package dev.halim.shelfdroid.download.storage.podcast

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.download.storage.ReadableStoragePolicy
import java.io.IOException
import java.io.OutputStream
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PodcastDurableDownloadCatalog
@Inject
constructor(
  @ApplicationContext context: Context,
  private val readableStoragePolicy: ReadableStoragePolicy,
) {
  private val resolver = context.contentResolver
  private val _changes = MutableStateFlow(0)
  val changes: StateFlow<Int> = _changes.asStateFlow()

  fun findEpisodeUri(podcastTitle: String, filename: String): Uri? {
    if (podcastTitle.isBlank() || filename.isBlank()) return null

    val projection = arrayOf(MediaStore.Downloads._ID)
    val selection =
      "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
    val selectionArgs = arrayOf(readableStoragePolicy.podcastRelativePath(podcastTitle), filename)

    resolver
      .query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null,
      )
      ?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
        return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
      }

    return null
  }

  fun deleteEpisode(podcastTitle: String, filename: String): Boolean {
    val uri = findEpisodeUri(podcastTitle, filename) ?: return false
    val deleted = resolver.delete(uri, null, null) > 0
    if (deleted) {
      bumpChanges()
    }
    return deleted
  }

  @Throws(IOException::class)
  fun writeEpisode(
    podcastTitle: String,
    filename: String,
    copyTo: (OutputStream) -> Unit,
  ): Uri {
    deleteEpisode(podcastTitle, filename)

    val values =
      ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(
          MediaStore.MediaColumns.RELATIVE_PATH,
          readableStoragePolicy.podcastRelativePath(podcastTitle),
        )
        put(MediaStore.MediaColumns.IS_PENDING, 1)
        URLConnection.guessContentTypeFromName(filename)?.let {
          put(MediaStore.MediaColumns.MIME_TYPE, it)
        }
      }

    val uri =
      resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("Failed to create shared storage entry for $filename")

    try {
      resolver.openOutputStream(uri, "w")?.use(copyTo)
        ?: throw IOException("Failed to open shared storage output stream for $filename")

      resolver.update(
        uri,
        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
        null,
        null,
      )
      bumpChanges()
      return uri
    } catch (error: Exception) {
      resolver.delete(uri, null, null)
      throw error
    }
  }

  private fun bumpChanges() {
    _changes.value += 1
  }
}
