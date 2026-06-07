package dev.halim.shelfdroid.download.storage.book

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
class BookDurableDownloadCatalog
@Inject
constructor(
  @ApplicationContext context: Context,
  private val readableStoragePolicy: ReadableStoragePolicy,
) {
  private val resolver = context.contentResolver
  private val folderSelectionPolicy = BookFolderSelectionPolicy()
  private val _changes = MutableStateFlow(0)
  val changes: StateFlow<Int> = _changes.asStateFlow()

  fun resolveRelativePath(
    bookTitle: String,
    author: String?,
    filenames: List<String>,
  ): String {
    val exactRelativePath = readableStoragePolicy.bookRelativePath(bookTitle, author)
    val requestedFilenames = filenames.filter { it.isNotBlank() }.toSet()
    if (requestedFilenames.isEmpty()) return exactRelativePath

    return folderSelectionPolicy.resolveRelativePath(
      exactRelativePath = exactRelativePath,
      filenames = requestedFilenames,
      matches = queryFolderMatches(requestedFilenames),
    )
  }

  fun trackUris(
    bookTitle: String,
    author: String?,
    filenames: List<String>,
  ): Map<String, Uri> {
    val requestedFilenames = filenames.filter { it.isNotBlank() }.distinct()
    if (requestedFilenames.isEmpty()) return emptyMap()

    val relativePath = resolveRelativePath(bookTitle, author, requestedFilenames)
    return requestedFilenames
      .mapNotNull { filename ->
        findTrackUri(relativePath, filename)?.let { filename to it }
      }
      .toMap()
  }

  fun findTrackUri(relativePath: String, filename: String): Uri? {
    if (relativePath.isBlank() || filename.isBlank()) return null

    val projection = arrayOf(MediaStore.Downloads._ID)
    val selection =
      "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
    val selectionArgs = arrayOf(relativePath, filename)

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

  fun deleteBookFolderContents(relativePath: String): Int {
    if (relativePath.isBlank()) return 0

    val deleted =
      resolver.delete(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
        arrayOf(relativePath),
      )
    if (deleted > 0) {
      bumpChanges()
    }
    return deleted
  }

  fun deleteBook(bookTitle: String, author: String?, filenames: List<String>): Boolean {
    val relativePath = resolveRelativePath(bookTitle, author, filenames)
    return deleteBookFolderContents(relativePath) > 0
  }

  @Throws(IOException::class)
  fun writeTrack(relativePath: String, filename: String, copyTo: (OutputStream) -> Unit): Uri {
    deleteTrack(relativePath, filename)

    val values =
      ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
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

  private fun deleteTrack(relativePath: String, filename: String): Boolean {
    val uri = findTrackUri(relativePath, filename) ?: return false
    val deleted = resolver.delete(uri, null, null) > 0
    if (deleted) {
      bumpChanges()
    }
    return deleted
  }

  private fun queryFolderMatches(filenames: Set<String>): List<BookFolderMatch> {
    if (filenames.isEmpty()) return emptyList()

    val projection =
      arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
    val placeholders = List(filenames.size) { "?" }.joinToString(",")
    val selection =
      "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
        "${MediaStore.MediaColumns.DISPLAY_NAME} IN ($placeholders)"
    val selectionArgs =
      arrayOf("${readableStoragePolicy.booksRootRelativePath()}%", *filenames.toTypedArray())

    val matches = mutableListOf<BookFolderMatch>()
    resolver
      .query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null,
      )
      ?.use { cursor ->
        val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
        val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        while (cursor.moveToNext()) {
          matches +=
            BookFolderMatch(
              relativePath = cursor.getString(relativePathIndex),
              filename = cursor.getString(displayNameIndex),
            )
        }
      }
    return matches
  }

  private fun bumpChanges() {
    _changes.value += 1
  }
}
