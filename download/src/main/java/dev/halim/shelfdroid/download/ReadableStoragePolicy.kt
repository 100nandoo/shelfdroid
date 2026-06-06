package dev.halim.shelfdroid.download

import android.os.Environment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadableStoragePolicy @Inject constructor() {

  fun podcastRelativePath(podcastTitle: String): String {
    return "${Environment.DIRECTORY_DOWNLOADS}/ShelfDroid/podcasts/${sanitizePathSegment(podcastTitle)}/"
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
