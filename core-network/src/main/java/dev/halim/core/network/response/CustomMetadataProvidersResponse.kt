package dev.halim.core.network.response

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

/**
 * The server intentionally omits the authorization header from its normal client projection. The
 * nullable field lets controlled fakes and compatible server versions exercise the conceal/reveal
 * presentation without persisting the value in ShelfDroid.
 */
@Serializable
data class CustomMetadataProvider(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("url") val url: String = "",
  @SerialName("mediaType") val mediaType: String = "book",
  @SerialName("slug") val slug: String = "",
  @SerialName("authHeaderValue") val authHeaderValue: String? = null,
)
