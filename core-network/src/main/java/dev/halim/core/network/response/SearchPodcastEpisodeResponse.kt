package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchPodcastEpisodeResponse(
  @SerialName("episodes") val episodes: List<SearchPodcastEpisodeMatch> = emptyList(),
)

@Serializable
data class SearchPodcastEpisodeMatch(@SerialName("episode") val episode: Episode)
