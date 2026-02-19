package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
  @SerialName("username") val username: String,
  @SerialName("email") val email: String,
  @SerialName("password") val password: String,
  @SerialName("type") val type: String,
  @SerialName("isActive") val isActive: Boolean,
  @SerialName("permissions") val permissions: Permissions,
  @SerialName("librariesAccessible") val librariesAccessible: List<String>,
  @SerialName("itemTagsSelected") val itemTagsSelected: List<String>,
) {
  @Serializable
  data class Permissions(
    @SerialName("download") val download: Boolean,
    @SerialName("update") val update: Boolean,
    @SerialName("delete") val delete: Boolean,
    @SerialName("upload") val upload: Boolean,
    @SerialName("createEreader") val createEreader: Boolean,
    @SerialName("accessExplicitContent") val accessExplicitContent: Boolean,
    @SerialName("accessAllLibraries") val accessAllLibraries: Boolean,
    @SerialName("accessAllTags") val accessAllTags: Boolean,
    @SerialName("selectedTagsNotAccessible") val selectedTagsNotAccessible: Boolean,
  )
}
