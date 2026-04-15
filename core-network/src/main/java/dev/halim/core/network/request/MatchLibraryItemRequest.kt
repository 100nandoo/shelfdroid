package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchLibraryItemRequest(
  @SerialName("provider") val provider: String,
  @SerialName("title") val title: String,
  @SerialName("author") val author: String? = null,
  @SerialName("overrideDefaults") val overrideDefaults: Boolean = false,
)

@Serializable data class CoverFromUrlRequest(@SerialName("url") val url: String)
