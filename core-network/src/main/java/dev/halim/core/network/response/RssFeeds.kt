package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RssFeedsResponse(@SerialName("feeds") val feeds: List<RssFeed> = emptyList())

@Serializable
data class RssFeed(
  @SerialName("id") val id: String,
  @SerialName("slug") val slug: String = "",
  @SerialName("entityType") val entityType: String = "",
  @SerialName("feedUrl") val feedUrl: String = "",
  @SerialName("serverAddress") val serverAddress: String? = null,
  @SerialName("updatedAt") val updatedAt: Long = 0L,
  @SerialName("meta") val meta: RssFeedMeta = RssFeedMeta(),
  @SerialName("episodes") val episodes: List<RssFeedEpisode> = emptyList(),
)

@Serializable
data class RssFeedMeta(
  @SerialName("title") val title: String = "",
  @SerialName("preventIndexing") val preventIndexing: Boolean = true,
  @SerialName("ownerName") val ownerName: String? = null,
  @SerialName("ownerEmail") val ownerEmail: String? = null,
)

@Serializable
data class RssFeedEpisode(
  @SerialName("id") val id: String,
  @SerialName("title") val title: String = "",
  @SerialName("pubDate") val pubDate: String? = null,
)
