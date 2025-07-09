package dev.halim.shelfdroid.media.mediaitem

data class MediaIdWrapper(val itemId: String, val episodeId: String? = null) {
  fun toMediaId(): String = episodeId?.let { "$itemId|$it" } ?: itemId

  companion object {
    fun fromMediaId(mediaId: String): MediaIdWrapper {
      val parts = mediaId.split("|")
      return if (parts.size == 2) MediaIdWrapper(parts[0], parts[1])
      else MediaIdWrapper(parts[0], null)
    }
  }
}
