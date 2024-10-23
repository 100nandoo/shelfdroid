package dev.halim.shelfdroid.network.login


import dev.halim.shelfdroid.network.AudioBookmark
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("bookmarks")
    val bookmarks: List<AudioBookmark> = listOf(),
    @SerialName("createdAt")
    val createdAt: Long = 0,
    @SerialName("email")
    val email: String? = "",
    @SerialName("hasOpenIDLink")
    val hasOpenIDLink: Boolean = false,
    @SerialName("id")
    val id: String = "",
    @SerialName("isActive")
    val isActive: Boolean = false,
    @SerialName("isLocked")
    val isLocked: Boolean = false,
    @SerialName("itemTagsSelected")
    val itemTagsSelected: List<String> = listOf(),
    @SerialName("lastSeen")
    val lastSeen: Long = 0,
    @SerialName("librariesAccessible")
    val librariesAccessible: List<String> = listOf(),
    @SerialName("mediaProgress")
    val mediaProgress: List<MediaProgres> = listOf(),
    @SerialName("permissions")
    val permissions: Permissions = Permissions(),
    @SerialName("seriesHideFromContinueListening")
    val seriesHideFromContinueListening: List<String> = listOf(),
    @SerialName("token")
    val token: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("username")
    val username: String = ""
)