package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault

@Serializable
data class CreateCustomMetadataProviderRequest(
  @SerialName("name") val name: String,
  @SerialName("url") val url: String,
  @EncodeDefault(EncodeDefault.Mode.ALWAYS)
  @SerialName("mediaType") val mediaType: String = "book",
  @SerialName("authHeaderValue") val authHeaderValue: String? = null,
)
