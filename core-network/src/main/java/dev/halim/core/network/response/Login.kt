package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
  @SerialName("user") val user: User = User(),
  @SerialName("userDefaultLibraryId") val userDefaultLibraryId: String = "",
  @SerialName("serverSettings") val serverSettings: ServerSettings = ServerSettings(),
  @SerialName("Source") val source: String = "",
)

@Serializable
data class User(
  @SerialName("id") val id: String = "",
  @SerialName("username") val username: String = "",
  @SerialName("type") val type: String = "",
  @SerialName("token") val token: String = "",
  @SerialName("mediaProgress") val mediaProgress: List<MediaProgress> = listOf(),
  @SerialName("seriesHideFromContinueListening")
  val seriesHideFromContinueListening: List<String> = listOf(),
  @SerialName("bookmarks") val bookmarks: List<AudioBookmark> = listOf(),
  @SerialName("isActive") val isActive: Boolean = false,
  @SerialName("isLocked") val isLocked: Boolean = false,
  @SerialName("lastSeen") val lastSeen: Long? = null,
  @SerialName("createdAt") val createdAt: Long = 0,
  @SerialName("permissions") val permissions: Permissions = Permissions(),
  @SerialName("librariesAccessible") val librariesAccessible: List<String> = listOf(),
  @SerialName("itemTagsAccessible") val itemTagsAccessible: List<String> = listOf(),
)

@Serializable
data class MediaProgress(
  @SerialName("id") val id: String = "",
  @SerialName("libraryItemId") val libraryItemId: String = "",
  @SerialName("episodeId") val episodeId: String? = null,
  @SerialName("mediaItemType") val mediaItemType: String = "",
  @SerialName("duration") val duration: Float = 0f,
  @SerialName("progress") val progress: Float = 0f,
  @SerialName("currentTime") val currentTime: Float = 0f,
  @SerialName("isFinished") val isFinished: Boolean = false,
  @SerialName("hideFromContinueListening") val hideFromContinueListening: Boolean = false,
  @SerialName("lastUpdate") val lastUpdate: Long = 0,
  @SerialName("startedAt") val startedAt: Long = 0,
  @SerialName("finishedAt") val finishedAt: Long? = null,
)

@Serializable
data class AudioBookmark(
  @SerialName("libraryItemId") val libraryItemId: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("time") val time: Int = 0,
  @SerialName("createdAt") val createdAt: Long = 0,
)

@Serializable
data class Permissions(
  @SerialName("download") val download: Boolean = false,
  @SerialName("update") val update: Boolean = false,
  @SerialName("delete") val delete: Boolean = false,
  @SerialName("upload") val upload: Boolean = false,
  @SerialName("accessAllLibraries") val accessAllLibraries: Boolean = false,
  @SerialName("accessAllTags") val accessAllTags: Boolean = false,
  @SerialName("accessExplicitContent") val accessExplicitContent: Boolean = false,
)

@Serializable
data class ServerSettings(
  @SerialName("id") val id: String = "",
  @SerialName("scannerFindCovers") val scannerFindCovers: Boolean = false,
  @SerialName("scannerCoverProvider") val scannerCoverProvider: String = "",
  @SerialName("scannerParseSubtitle") val scannerParseSubtitle: Boolean = false,
  @SerialName("scannerPreferMatchedMetadata") val scannerPreferMatchedMetadata: Boolean = false,
  @SerialName("scannerDisableWatcher") val scannerDisableWatcher: Boolean = false,
  @SerialName("storeCoverWithItem") val storeCoverWithItem: Boolean = false,
  @SerialName("storeMetadataWithItem") val storeMetadataWithItem: Boolean = false,
  @SerialName("metadataFileFormat") val metadataFileFormat: String = "",
  @SerialName("rateLimitLoginRequests") val rateLimitLoginRequests: Int = 0,
  @SerialName("rateLimitLoginWindow") val rateLimitLoginWindow: Int = 0,
  @SerialName("backupSchedule") val backupSchedule: String = "",
  @SerialName("backupsToKeep") val backupsToKeep: Int = 0,
  @SerialName("maxBackupSize") val maxBackupSize: Int = 0,
  @SerialName("loggerDailyLogsToKeep") val loggerDailyLogsToKeep: Int = 0,
  @SerialName("loggerScannerLogsToKeep") val loggerScannerLogsToKeep: Int = 0,
  @SerialName("homeBookshelfView") val homeBookshelfView: Int = 0,
  @SerialName("bookshelfView") val bookshelfView: Int = 0,
  @SerialName("sortingIgnorePrefix") val sortingIgnorePrefix: Boolean = false,
  @SerialName("sortingPrefixes") val sortingPrefixes: List<String> = listOf(),
  @SerialName("chromecastEnabled") val chromecastEnabled: Boolean = false,
  @SerialName("dateFormat") val dateFormat: String = "",
  @SerialName("language") val language: String = "",
  @SerialName("logLevel") val logLevel: Int = 0,
  @SerialName("version") val version: String = "",
)
