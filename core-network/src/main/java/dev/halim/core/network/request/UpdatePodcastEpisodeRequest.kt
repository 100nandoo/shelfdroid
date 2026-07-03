package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePodcastEpisodeRequest(
  @SerialName("season") val season: String? = null,
  @SerialName("episode") val episode: String? = null,
  @SerialName("episodeType") val episodeType: String? = null,
  @SerialName("title") val title: String? = null,
  @SerialName("subtitle") val subtitle: String? = null,
  @SerialName("description") val description: String? = null,
  @SerialName("pubDate") val pubDate: String? = null,
  @SerialName("publishedAt") val publishedAt: Long? = null,
)
