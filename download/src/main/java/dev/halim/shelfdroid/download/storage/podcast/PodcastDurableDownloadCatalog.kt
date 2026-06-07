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
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private data class PodcastEpisodeKey(val relativePath: String, val filename: String)

private data class PodcastCatalogSnapshot(
  val relativePathsByFilename: Map<String, List<String>>,
  val uriByEpisode: Map<PodcastEpisodeKey, Uri>,
)

@Singleton
class PodcastDurableDownloadCatalog
@Inject
constructor(
  @ApplicationContext context: Context,
  private val readableStoragePolicy: ReadableStoragePolicy,
  @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
) {
  private val resolver = context.contentResolver
  private val folderSelectionPolicy = PodcastFolderSelectionPolicy()
  private val _changes = MutableStateFlow(0)
  val changes: StateFlow<Int> = _changes.asStateFlow()
  @Volatile private var cachedSnapshot: PodcastCatalogSnapshot? = null

  suspend fun findEpisodeUri(podcastTitle: String, filename: String): Uri? {
    return withContext(ioDispatcher) { findEpisodeUriInternal(podcastTitle, filename) }
  }

  private fun findEpisodeUriInternal(podcastTitle: String, filename: String): Uri? {
    if (podcastTitle.isBlank() || filename.isBlank()) return null

    val resolvedRelativePath =
      folderSelectionPolicy.resolveRelativePath(
        exactRelativePath = readableStoragePolicy.podcastRelativePath(podcastTitle),
        filename = filename,
        matches = queryRelativePathMatches(filename),
      ) ?: return null

    return findEpisodeUriAtPathInternal(resolvedRelativePath, filename)
  }

  private fun findEpisodeUriAtPathInternal(relativePath: String, filename: String): Uri? {
    if (relativePath.isBlank() || filename.isBlank()) return null
    return snapshot().uriByEpisode[PodcastEpisodeKey(relativePath, filename)]
  }

  private fun queryRelativePathMatches(filename: String): List<String> {
    if (filename.isBlank()) return emptyList()
    return snapshot().relativePathsByFilename[filename].orEmpty()
  }

  suspend fun deleteEpisode(podcastTitle: String, filename: String): Boolean {
    return withContext(ioDispatcher) { deleteEpisodeInternal(podcastTitle, filename) }
  }

  private fun deleteEpisodeInternal(podcastTitle: String, filename: String): Boolean {
    val uri = findEpisodeUriInternal(podcastTitle, filename) ?: return false
    val deleted = resolver.delete(uri, null, null) > 0
    if (deleted) {
      bumpChanges()
    }
    return deleted
  }

  @Throws(IOException::class)
  suspend fun writeEpisode(
    podcastTitle: String,
    filename: String,
    copyTo: (OutputStream) -> Unit,
  ): Uri {
    return withContext(ioDispatcher) { writeEpisodeInternal(podcastTitle, filename, copyTo) }
  }

  @Throws(IOException::class)
  private fun writeEpisodeInternal(
    podcastTitle: String,
    filename: String,
    copyTo: (OutputStream) -> Unit,
  ): Uri {
    deleteEpisodeInternal(podcastTitle, filename)

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
    cachedSnapshot = null
    _changes.value += 1
  }

  private fun snapshot(): PodcastCatalogSnapshot {
    cachedSnapshot?.let {
      return it
    }

    return synchronized(this) {
      cachedSnapshot ?: loadSnapshot().also { cachedSnapshot = it }
    }
  }

  private fun loadSnapshot(): PodcastCatalogSnapshot {
    val projection =
      arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.DISPLAY_NAME,
      )
    val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("${readableStoragePolicy.podcastsRootRelativePath()}%")

    val relativePathsByFilename = mutableMapOf<String, MutableList<String>>()
    val uriByEpisode = mutableMapOf<PodcastEpisodeKey, Uri>()

    resolver
      .query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null,
      )
      ?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
        val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
        val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        while (cursor.moveToNext()) {
          val relativePath = cursor.getString(relativePathIndex)
          val filename = cursor.getString(displayNameIndex)
          val uri =
            ContentUris.withAppendedId(
              MediaStore.Downloads.EXTERNAL_CONTENT_URI,
              cursor.getLong(idIndex),
            )

          relativePathsByFilename.getOrPut(filename) { mutableListOf() }.add(relativePath)
          uriByEpisode[PodcastEpisodeKey(relativePath, filename)] = uri
        }
      }

    return PodcastCatalogSnapshot(
      relativePathsByFilename = relativePathsByFilename.mapValues { (_, paths) -> paths.toList() },
      uriByEpisode = uriByEpisode,
    )
  }
}
