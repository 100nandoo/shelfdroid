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
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private data class BookTrackKey(val relativePath: String, val filename: String)

private data class BookCatalogSnapshot(
  val relativePathsByFilename: Map<String, Set<String>>,
  val uriByTrack: Map<BookTrackKey, Uri>,
)

@Singleton
class BookDurableDownloadCatalog
@Inject
constructor(
  @ApplicationContext context: Context,
  private val readableStoragePolicy: ReadableStoragePolicy,
  @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
) {
  private val resolver = context.contentResolver
  private val folderSelectionPolicy = BookFolderSelectionPolicy()
  private val _changes = MutableStateFlow(0)
  val changes: StateFlow<Int> = _changes.asStateFlow()
  @Volatile private var cachedSnapshot: BookCatalogSnapshot? = null

  suspend fun resolveRelativePath(
    bookTitle: String,
    author: String?,
    filenames: List<String>,
  ): String {
    return withContext(ioDispatcher) { resolveRelativePathInternal(bookTitle, author, filenames) }
  }

  private fun resolveRelativePathInternal(
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

  suspend fun trackUris(
    bookTitle: String,
    author: String?,
    filenames: List<String>,
  ): Map<String, Uri> {
    return withContext(ioDispatcher) { trackUrisInternal(bookTitle, author, filenames) }
  }

  private fun trackUrisInternal(
    bookTitle: String,
    author: String?,
    filenames: List<String>,
  ): Map<String, Uri> {
    val requestedFilenames = filenames.filter { it.isNotBlank() }.distinct()
    if (requestedFilenames.isEmpty()) return emptyMap()

    val relativePath = resolveRelativePathInternal(bookTitle, author, requestedFilenames)
    return requestedFilenames
      .mapNotNull { filename ->
        findTrackUriInternal(relativePath, filename)?.let { filename to it }
      }
      .toMap()
  }

  suspend fun findTrackUri(relativePath: String, filename: String): Uri? {
    return withContext(ioDispatcher) { findTrackUriInternal(relativePath, filename) }
  }

  private fun findTrackUriInternal(relativePath: String, filename: String): Uri? {
    if (relativePath.isBlank() || filename.isBlank()) return null
    return snapshot().uriByTrack[BookTrackKey(relativePath, filename)]
  }

  suspend fun deleteBookFolderContents(relativePath: String): Int {
    return withContext(ioDispatcher) { deleteBookFolderContentsInternal(relativePath) }
  }

  private fun deleteBookFolderContentsInternal(relativePath: String): Int {
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

  suspend fun deleteBook(bookTitle: String, author: String?, filenames: List<String>): Boolean {
    return withContext(ioDispatcher) {
      val relativePath = resolveRelativePathInternal(bookTitle, author, filenames)
      deleteBookFolderContentsInternal(relativePath) > 0
    }
  }

  @Throws(IOException::class)
  suspend fun writeTrack(
    relativePath: String,
    filename: String,
    copyTo: (OutputStream) -> Unit,
  ): Uri {
    return withContext(ioDispatcher) { writeTrackInternal(relativePath, filename, copyTo) }
  }

  @Throws(IOException::class)
  private fun writeTrackInternal(
    relativePath: String,
    filename: String,
    copyTo: (OutputStream) -> Unit,
  ): Uri {
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
    val uri = findTrackUriInternal(relativePath, filename) ?: return false
    val deleted = resolver.delete(uri, null, null) > 0
    if (deleted) {
      bumpChanges()
    }
    return deleted
  }

  private fun queryFolderMatches(filenames: Set<String>): List<BookFolderMatch> {
    if (filenames.isEmpty()) return emptyList()
    val relativePathsByFilename = snapshot().relativePathsByFilename
    return filenames.flatMap { filename ->
      relativePathsByFilename[filename].orEmpty().map { relativePath ->
        BookFolderMatch(relativePath = relativePath, filename = filename)
      }
    }
  }

  private fun bumpChanges() {
    cachedSnapshot = null
    _changes.value += 1
  }

  private fun snapshot(): BookCatalogSnapshot {
    cachedSnapshot?.let {
      return it
    }

    return synchronized(this) {
      cachedSnapshot ?: loadSnapshot().also { cachedSnapshot = it }
    }
  }

  private fun loadSnapshot(): BookCatalogSnapshot {
    val projection =
      arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.DISPLAY_NAME,
      )
    val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("${readableStoragePolicy.booksRootRelativePath()}%")

    val relativePathsByFilename = mutableMapOf<String, MutableSet<String>>()
    val uriByTrack = mutableMapOf<BookTrackKey, Uri>()

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

          relativePathsByFilename.getOrPut(filename) { linkedSetOf() }.add(relativePath)
          uriByTrack[BookTrackKey(relativePath, filename)] = uri
        }
      }

    return BookCatalogSnapshot(
      relativePathsByFilename = relativePathsByFilename.mapValues { (_, paths) -> paths.toSet() },
      uriByTrack = uriByTrack,
    )
  }
}
