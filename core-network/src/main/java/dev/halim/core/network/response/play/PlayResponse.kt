package dev.halim.core.network.response.play

import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.core.network.response.libraryitem.Metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = PlaySerializer::class)
data class PlayResponse(
  @SerialName("id") val id: String = "",
  @SerialName("userId") val userId: String = "",
  @SerialName("libraryId") val libraryId: String = "",
  @SerialName("libraryItemId") val libraryItemId: String = "",
  @SerialName("bookId") val bookId: String = "",
  @SerialName("episodeId") val episodeId: String? = null,
  @SerialName("mediaType") val mediaType: String = "",
  @SerialName("mediaMetadata") val mediaMetadata: Metadata,
  @SerialName("chapters") val chapters: List<BookChapter> = listOf(),
  @SerialName("displayTitle") val displayTitle: String = "",
  @SerialName("displayAuthor") val displayAuthor: String = "",
  @SerialName("coverPath") val coverPath: String = "",
  @SerialName("duration") val duration: Double = 0.0,
  @SerialName("playMethod") val playMethod: Int = 0,
  @SerialName("mediaPlayer") val mediaPlayer: String = "",
  @SerialName("deviceInfo") val deviceInfo: DeviceInfo = DeviceInfo(),
  @SerialName("serverVersion") val serverVersion: String = "",
  @SerialName("date") val date: String = "",
  @SerialName("dayOfWeek") val dayOfWeek: String = "",
  @SerialName("timeListening") val timeListening: Int = 0,
  @SerialName("startTime") val startTime: Double = 0.0,
  @SerialName("currentTime") val currentTime: Double = 0.0,
  @SerialName("startedAt") val startedAt: Long = 0,
  @SerialName("updatedAt") val updatedAt: Long = 0,
  @SerialName("audioTracks") val audioTracks: List<AudioTrack> = listOf(),
  @SerialName("libraryItem") val libraryItem: LibraryItem,
)

@Serializable
data class DeviceInfo(
  @SerialName("id") val id: String = "",
  @SerialName("userId") val userId: String = "",
  @SerialName("deviceId") val deviceId: String = "",
  @SerialName("ipAddress") val ipAddress: String = "",
  @SerialName("clientVersion") val clientVersion: String = "",
  @SerialName("clientName") val clientName: String = "",
)

@Serializable
data class AudioTrack(
  @SerialName("index") val index: Int = 0,
  @SerialName("ino") val ino: String = "",
  @SerialName("metadata") val metadata: FileMetadata = FileMetadata(),
  @SerialName("addedAt") val addedAt: Long = 0,
  @SerialName("updatedAt") val updatedAt: Long = 0,
  @SerialName("discNumFromFilename") val discNumFromFilename: Int = 0,
  @SerialName("manuallyVerified") val manuallyVerified: Boolean = false,
  @SerialName("exclude") val exclude: Boolean = false,
  @SerialName("format") val format: String = "",
  @SerialName("duration") val duration: Double = 0.0,
  @SerialName("bitRate") val bitRate: Int = 0,
  @SerialName("codec") val codec: String = "",
  @SerialName("timeBase") val timeBase: String = "",
  @SerialName("channels") val channels: Int = 0,
  @SerialName("channelLayout") val channelLayout: String = "",
  @SerialName("mimeType") val mimeType: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("startOffset") val startOffset: Double = 0.0,
  @SerialName("contentUrl") val contentUrl: String = "",
)
