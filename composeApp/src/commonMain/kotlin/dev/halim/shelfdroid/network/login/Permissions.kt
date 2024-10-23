package dev.halim.shelfdroid.network.login


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Permissions(
    @SerialName("accessAllLibraries")
    val accessAllLibraries: Boolean = false,
    @SerialName("accessAllTags")
    val accessAllTags: Boolean = false,
    @SerialName("accessExplicitContent")
    val accessExplicitContent: Boolean = false,
    @SerialName("delete")
    val delete: Boolean = false,
    @SerialName("download")
    val download: Boolean = false,
    @SerialName("update")
    val update: Boolean = false,
    @SerialName("upload")
    val upload: Boolean = false
)