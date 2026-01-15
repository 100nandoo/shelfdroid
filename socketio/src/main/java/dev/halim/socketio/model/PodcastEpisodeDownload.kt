package dev.halim.socketio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PodcastEpisodeDownload(
  @SerialName("id") val id: String,
  @SerialName("episodeDisplayTitle") val episodeDisplayTitle: String,
  @SerialName("url") val url: String,
  @SerialName("libraryItemId") val libraryItemId: String,
  @SerialName("libraryId") val libraryId: String,
  @SerialName("isFinished") val isFinished: Boolean,
  @SerialName("failed") val failed: Boolean,
  @SerialName("startedAt") val startedAt: Long?,
  @SerialName("createdAt") val createdAt: Long,
  @SerialName("finishedAt") val finishedAt: Long?,
  @SerialName("podcastTitle") val podcastTitle: String?,
  @SerialName("podcastExplicit") val podcastExplicit: Boolean,
  @SerialName("season") val season: String?,
  @SerialName("episode") val episode: String?,
  @SerialName("episodeType") val episodeType: String,
  @SerialName("publishedAt") val publishedAt: Long?,
)
