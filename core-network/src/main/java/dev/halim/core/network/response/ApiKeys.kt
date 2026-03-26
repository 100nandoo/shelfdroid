package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiKeysResponse(@SerialName("apiKeys") val apiKeys: List<ApiKey>) {
  @Serializable
  data class ApiKey(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("lastUsedAt") val lastUsedAt: String?,
    @SerialName("isActive") val isActive: Boolean,
    @SerialName("permissions") val permissions: String?,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("userId") val userId: String,
    @SerialName("createdByUserId") val createdByUserId: String,
    @SerialName("user") val user: User,
    @SerialName("createdByUser") val createdByUser: User,
  ) {
    @Serializable
    data class User(
      @SerialName("id") val id: String,
      @SerialName("username") val username: String,
      @SerialName("type") val type: String,
    )
  }
}

@Serializable
data class CreateUpdateApiKeyResponse(@SerialName("apiKey") val apiKey: ApiKey) {
  @Serializable
  data class ApiKey(
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("userId") val userId: String,
    @SerialName("isActive") val isActive: Boolean,
    @SerialName("createdByUserId") val createdByUserId: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("user") val user: ApiKeysResponse.ApiKey.User,
  )
}
