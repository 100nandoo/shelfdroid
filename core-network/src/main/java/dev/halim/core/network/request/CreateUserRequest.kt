package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
  @SerialName("username") val username: String = "",
  @SerialName("password") val password: String = "",
  @SerialName("type") val type: String = "",
  @SerialName("isActive") val isActive: Boolean = false,
  @SerialName("email") val email: String? = null,
  @SerialName("permissions") val permissions: UpdateUserRequest.Permissions,
  @SerialName("librariesAccessible") val librariesAccessible: List<String> = emptyList(),
  @SerialName("itemTagsAccessible") val itemTagsAccessible: List<String> = emptyList(),
)
