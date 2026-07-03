package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePodcastEpisodeEnclosureRequest(
  @SerialName("url") val url: String,
  @SerialName("type") val type: String? = null,
  @SerialName("length") val length: String? = null,
)

@Serializable
data class UpdatePodcastEpisodeRequest(
  @SerialName("season") val season: String? = null,
  @SerialName("episode") val episode: String? = null,
  @SerialName("episodeType") val episodeType: String? = null,
  @SerialName("title") val title: String? = null,
  @SerialName("subtitle") val subtitle: String? = null,
  @SerialName("description") val description: String? = null,
  @SerialName("enclosure") val enclosure: UpdatePodcastEpisodeEnclosureRequest? = null,
  @SerialName("pubDate") val pubDate: String? = null,
  @SerialName("publishedAt") val publishedAt: Long? = null,
)
