package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchProvidersResponse(
  @SerialName("providers") val providers: SearchProviders = SearchProviders()
)

@Serializable
data class SearchProviders(
  @SerialName("books") val books: List<SearchProvider> = emptyList(),
  @SerialName("booksCovers") val booksCovers: List<SearchProvider> = emptyList(),
  @SerialName("podcasts") val podcasts: List<SearchProvider> = emptyList(),
)

@Serializable
data class SearchProvider(
  @SerialName("value") val value: String = "",
  @SerialName("text") val text: String = "",
)
