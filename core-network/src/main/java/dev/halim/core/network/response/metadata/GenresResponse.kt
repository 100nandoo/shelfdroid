package dev.halim.core.network.response.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenresResponse(@SerialName("genres") val genres: List<String> = emptyList())
