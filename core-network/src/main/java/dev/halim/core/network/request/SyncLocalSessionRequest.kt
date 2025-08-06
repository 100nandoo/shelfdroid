package dev.halim.core.network.request

import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncLocalSessionRequest(
  @SerialName("id") val id: String,
  @SerialName("userId") val userId: String,
  @SerialName("libraryId") val libraryId: String,
  @SerialName("libraryItemId") val libraryItemId: String,
  @SerialName("episodeId") val episodeId: String? = null,
  @SerialName("mediaType") val mediaType: String,
  @SerialName("mediaMetadata") val mediaMetadata: Metadata,
  @SerialName("chapters") val chapters: List<BookChapter>,
  @SerialName("displayTitle") val displayTitle: String,
  @SerialName("displayAuthor") val displayAuthor: String,
  @SerialName("coverPath") val coverPath: String,
  @SerialName("duration") val duration: Double,
  @SerialName("playMethod") val playMethod: Int,
  @SerialName("mediaPlayer") val mediaPlayer: String,
  @SerialName("deviceInfo") val deviceInfo: DeviceInfo,
  @SerialName("serverVersion") val serverVersion: String,
  @SerialName("date") val date: String,
  @SerialName("dayOfWeek") val dayOfWeek: String,
  @SerialName("timeListening") val timeListening: Int,
  @SerialName("startTime") val startTime: Double,
  @SerialName("currentTime") val currentTime: Double,
  @SerialName("startedAt") val startedAt: Long,
  @SerialName("updatedAt") val updatedAt: Long,
)
