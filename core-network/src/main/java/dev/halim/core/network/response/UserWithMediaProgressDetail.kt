package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserWithMediaProgressDetail(
  @SerialName("id") val id: String,
  @SerialName("username") val username: String,
  @SerialName("email") val email: String?,
  @SerialName("type") val type: String,
  @SerialName("token") val token: String,
  @SerialName("mediaProgress") val mediaProgress: List<MediaProgress>,
  @SerialName("seriesHideFromContinueListening") val seriesHideFromContinueListening: List<String>,
  @SerialName("bookmarks") val bookmarks: List<Bookmark>,
  @SerialName("isActive") val isActive: Boolean,
  @SerialName("isLocked") val isLocked: Boolean,
  @SerialName("lastSeen") val lastSeen: Long,
  @SerialName("createdAt") val createdAt: Long,
  @SerialName("permissions") val permissions: Permissions,
  @SerialName("librariesAccessible") val librariesAccessible: List<String>,
  @SerialName("itemTagsSelected") val itemTagsSelected: List<String>,
  @SerialName("hasOpenIDLink") val hasOpenIDLink: Boolean,
) {
  @Serializable
  data class MediaProgress(
    @SerialName("id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("libraryItemId") val libraryItemId: String,
    @SerialName("episodeId") val episodeId: String?,
    @SerialName("mediaItemId") val mediaItemId: String,
    @SerialName("mediaItemType") val mediaItemType: String,
    @SerialName("duration") val duration: Double,
    @SerialName("progress") val progress: Double,
    @SerialName("currentTime") val currentTime: Double,
    @SerialName("isFinished") val isFinished: Boolean,
    @SerialName("hideFromContinueListening") val hideFromContinueListening: Boolean,
    @SerialName("ebookLocation") val ebookLocation: String?,
    @SerialName("ebookProgress") val ebookProgress: Int?,
    @SerialName("lastUpdate") val lastUpdate: Long,
    @SerialName("startedAt") val startedAt: Long,
    @SerialName("finishedAt") val finishedAt: Long?,
    @SerialName("displayTitle") val displayTitle: String,
    @SerialName("coverPath") val coverPath: String,
    @SerialName("mediaUpdatedAt") val mediaUpdatedAt: String,
    @SerialName("displaySubtitle") val displaySubtitle: String? = "",
  )

  @Serializable
  data class Bookmark(
    @SerialName("libraryItemId") val libraryItemId: String,
    @SerialName("time") val time: Int,
    @SerialName("title") val title: String,
    @SerialName("createdAt") val createdAt: Long,
  )

  @Serializable
  data class Permissions(
    @SerialName("download") val download: Boolean,
    @SerialName("update") val update: Boolean,
    @SerialName("delete") val delete: Boolean,
    @SerialName("upload") val upload: Boolean,
    @SerialName("accessAllLibraries") val accessAllLibraries: Boolean,
    @SerialName("accessAllTags") val accessAllTags: Boolean,
    @SerialName("accessExplicitContent") val accessExplicitContent: Boolean,
  )
}
