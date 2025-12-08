package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable
data class CreatePodcastRequest(
  val path: String,
  val folderId: String,
  val libraryId: String,
  val media: Media,
) {
  @Serializable
  data class Media(val metadata: Metadata, val autoDownloadEpisodes: Boolean = false) {
    @Serializable
    data class Metadata(
      val title: String?,
      val author: String?,
      val description: String?,
      val releaseDate: String?,
      val genres: List<String>,
      val feedUrl: String?,
      val imageUrl: String?,
      val itunesPageUrl: String?,
      val itunesId: Int?,
      val itunesArtistId: Int?,
      val language: String?,
      val explicit: Boolean = false,
      val type: String?,
    )
  }
}
