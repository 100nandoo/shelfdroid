package dev.halim.core.network.response

import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionsResponse(
  @SerialName("total") val total: Int,
  @SerialName("numPages") val numPages: Int,
  @SerialName("page") val page: Int,
  @SerialName("itemsPerPage") val itemsPerPage: Int,
  @SerialName("sessions") val sessions: List<Session>,
)

@Serializable
data class Session(
  @SerialName("id") val id: String,
  @SerialName("userId") val userId: String?,
  @SerialName("libraryId") val libraryId: String?,
  @SerialName("libraryItemId") val libraryItemId: String?,
  @SerialName("bookId") val bookId: String?,
  @SerialName("episodeId") val episodeId: String?,
  @SerialName("mediaType") val mediaType: String,
  @SerialName("mediaMetadata") val mediaMetadata: Metadata,
  @SerialName("chapters") val chapters: List<BookChapter>,
  @SerialName("displayTitle") val displayTitle: String,
  @SerialName("displayAuthor") val displayAuthor: String,
  @SerialName("coverPath") val coverPath: String?,
  @SerialName("duration") val duration: Double,
  @SerialName("playMethod") val playMethod: Int,
  @SerialName("mediaPlayer") val mediaPlayer: String,
  @SerialName("deviceInfo") val deviceInfo: DeviceInfo,
  @SerialName("date") val date: String,
  @SerialName("dayOfWeek") val dayOfWeek: String,
  @SerialName("timeListening") val timeListening: Double?,
  @SerialName("startTime") val startTime: Double,
  @SerialName("currentTime") val currentTime: Double,
  @SerialName("startedAt") val startedAt: Long,
  @SerialName("updatedAt") val updatedAt: Long,
  @SerialName("user") val user: SessionUser? = null,
) {}

@Serializable
data class DeviceInfo(
  @SerialName("ipAddress") val ipAddress: String? = null,
  @SerialName("browserName") val browserName: String? = null,
  @SerialName("browserVersion") val browserVersion: String? = null,
  @SerialName("osName") val osName: String? = null,
  @SerialName("osVersion") val osVersion: String? = null,
  @SerialName("clientName") val clientName: String? = null,
  @SerialName("clientVersion") val clientVersion: String? = null,
  @SerialName("manufacturer") val manufacturer: String? = null,
  @SerialName("model") val model: String? = null,
  @SerialName("deviceName") val deviceName: String? = null,
)

@Serializable
data class SessionUser(
  @SerialName("id") val id: String?,
  @SerialName("username") val username: String,
)
