package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class PodcastFeed(@SerialName("podcast") val podcast: Podcast)

@Serializable
data class Podcast(
  @SerialName("metadata") val metadata: PodcastMetadata,
  @SerialName("episodes") val episodes: List<Episode>,
)

@Serializable
data class PodcastMetadata(
  @SerialName("image") val image: String,
  @SerialName("categories") val categories: List<String>,
  @SerialName("feedUrl") val feedUrl: String,
  @SerialName("description") val description: String,
  @SerialName("descriptionPlain") val descriptionPlain: String,
  @SerialName("type") val type: String,
  @SerialName("title") val title: String,
  @SerialName("language") val language: String,
  @SerialName("explicit") val explicit: String?,
  @SerialName("author") val author: String,
  @SerialName("pubDate") val pubDate: String?,
  @SerialName("link") val link: String,
)

@Serializable
data class Episode(
  @SerialName("title") val title: String,
  @SerialName("subtitle") val subtitle: String,
  @SerialName("description") val description: String,
  @SerialName("descriptionPlain") val descriptionPlain: String,
  @SerialName("pubDate") val pubDate: String,
  @SerialName("episodeType") val episodeType: String,
  @SerialName("season") val season: String,
  @SerialName("episode") val episode: String,
  @SerialName("author") val author: String,
  @SerialName("duration") val duration: String,
  @SerialName("explicit") val explicit: String,
  @SerialName("publishedAt") val publishedAt: Long,
  @SerialName("enclosure") val enclosure: Enclosure,
)

@Serializable
data class Enclosure(
  @SerialName("url") val url: String,
  @SerialName("type") val type: String,
  @SerialName("length") val length: String,
)
