package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePodcastEpisodeRequest(
  @SerialName("title") val title: String? = null,
)
