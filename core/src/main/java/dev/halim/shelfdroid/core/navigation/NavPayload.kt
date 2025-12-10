package dev.halim.shelfdroid.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class PodcastFeedNavPayload(
  val id: String = "",
  val itunesId: Int = 0,
  val itunesArtistId: Int? = null,
  val libraryId: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val genre: String = "",
  val episodeCount: Int = 0,
  val feedUrl: String = "",
  val pageUrl: String = "",
  val releaseDate: String = "",
  val explicit: Boolean = false,
  val isAdded: Boolean = false,
)
