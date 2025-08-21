package dev.halim.shelfdroid.media.mediaitem

/*
 Secondary id:
 - If its a book, it will be null if single track book, will be index if multiple track book
 - If it's a podcast, it will be episodeId
*/
data class MediaIdWrapper(val itemId: String, val secondaryId: String? = null) {
  fun toMediaId(): String = secondaryId?.let { "$itemId|$it" } ?: itemId

  companion object {
    fun fromMediaId(mediaId: String): MediaIdWrapper {
      val parts = mediaId.split("|")
      return if (parts.size == 2) MediaIdWrapper(parts[0], parts[1])
      else MediaIdWrapper(parts[0], null)
    }
  }
}
