package dev.halim.shelfdroid.media.library

import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

internal sealed class CarMediaId(val value: String) {
  sealed class Folder(value: String) : CarMediaId(value)

  data object Root : Folder("car:root")

  data object ContinueListening : Folder("car:continue")

  data object Books : Folder("car:books")

  data object Podcasts : Folder("car:podcasts")

  data object Downloads : Folder("car:downloads")

  data class Library(val libraryId: String) : CarMediaId("car:library:$libraryId")

  data class Book(val bookId: String) : CarMediaId("car:book:$bookId")

  data class Podcast(val podcastId: String) : CarMediaId("car:podcast:$podcastId")

  data class Episode(val podcastId: String, val episodeId: String) :
    CarMediaId("car:episode:$podcastId:$episodeId")

  // The catalog lookup determines whether the secondary ID names a Track or an Episode.
  data class LegacyPlayable(val id: MediaIdWrapper) : CarMediaId(id.toMediaId())

  companion object {
    /** Returns null for malformed IDs. The caller checks whether a parsed ID still exists. */
    fun parse(value: String): CarMediaId? {
      if (!value.startsWith("car:")) {
        val parts = value.split('|', limit = 3)
        if (parts.size > 2 || !parts.all(::isIdComponent)) return null
        return LegacyPlayable(MediaIdWrapper.fromMediaId(value))
      }

      val parts = value.split(':', limit = 5).drop(1)
      val kind = parts.first()
      val ids = parts.drop(1)
      if (!ids.all(::isIdComponent)) return null
      return when (ids.size) {
        0 ->
          when (kind) {
            "root" -> Root
            "continue" -> ContinueListening
            "books" -> Books
            "podcasts" -> Podcasts
            "downloads" -> Downloads
            else -> null
          }
        1 ->
          when (kind) {
            "library" -> Library(ids.single())
            "book" -> Book(ids.single())
            "podcast" -> Podcast(ids.single())
            else -> null
          }
        2 -> if (kind == "episode") Episode(ids[0], ids[1]) else null
        else -> null
      }
    }

    private fun isIdComponent(value: String): Boolean =
      value.isNotEmpty() &&
        value.none { it == ':' || it == '|' || it.isWhitespace() || it.isISOControl() }
  }
}
