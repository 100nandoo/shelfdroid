package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenItemRssFeedRequest(
  @SerialName("serverAddress") val serverAddress: String,
  @SerialName("slug") val slug: String,
  @SerialName("metadataDetails") val metadataDetails: OpenItemRssFeedMetadataDetails,
)

@Serializable
data class OpenItemRssFeedMetadataDetails(
  @SerialName("preventIndexing") val preventIndexing: Boolean = true,
  @SerialName("ownerName") val ownerName: String = "",
  @SerialName("ownerEmail") val ownerEmail: String = "",
)
