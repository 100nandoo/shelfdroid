package dev.halim.shelfdroid.download

import android.os.Environment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadableStoragePolicy @Inject constructor() {

  fun bookRelativePath(bookTitle: String, author: String?): String {
    val titleSegment = sanitizePathSegment(bookTitle)
    val authorSegment = sanitizePathSegment(author.orEmpty())
    return "${booksRootRelativePath()}${titleSegment}_${authorSegment}/"
  }

  fun podcastRelativePath(podcastTitle: String): String {
    return "${podcastsRootRelativePath()}${sanitizePathSegment(podcastTitle)}/"
  }

  fun booksRootRelativePath(): String {
    return "${Environment.DIRECTORY_DOWNLOADS}/ShelfDroid/books/"
  }

  fun podcastsRootRelativePath(): String {
    return "${Environment.DIRECTORY_DOWNLOADS}/ShelfDroid/podcasts/"
  }

  fun sanitizePathSegment(value: String): String {
    val sanitized =
      value
        .trim()
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""[\u0000-\u001f]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('.')

    return sanitized.ifBlank { "untitled" }
  }
}
