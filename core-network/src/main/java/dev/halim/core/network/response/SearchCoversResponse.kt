package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchCoversResponse(@SerialName("results") val results: List<String> = emptyList())
