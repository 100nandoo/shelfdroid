package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckNewEpisodesResponse(
  @SerialName("episodes") val episodes: List<Episode> = emptyList()
)
