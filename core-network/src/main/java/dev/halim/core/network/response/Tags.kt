package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class TagsResponse(@SerialName("tags") val tags: List<String> = emptyList())
