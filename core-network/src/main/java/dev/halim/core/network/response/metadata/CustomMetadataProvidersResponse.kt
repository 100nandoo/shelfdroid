package dev.halim.core.network.response.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomMetadataProvidersResponse(
  @SerialName("providers") val providers: List<CustomMetadataProvider> = emptyList()
)

@Serializable
data class CustomMetadataProviderResponse(
  @SerialName("provider") val provider: CustomMetadataProvider = CustomMetadataProvider()
)

@Serializable
data class CustomMetadataProvider(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("url") val url: String = "",
  @SerialName("mediaType") val mediaType: String = "book",
  @SerialName("slug") val slug: String = "",
  @SerialName("authHeaderValue") val authHeaderValue: String? = null,
)
