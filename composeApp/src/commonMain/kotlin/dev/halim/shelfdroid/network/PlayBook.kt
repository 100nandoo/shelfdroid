package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.network.libraryitem.BookMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfoRequest(
    val deviceId: String, val clientName: String, val clientVersion: String,
    val manufacturer: String, val model: String, val sdkVersion: Int
)

@Serializable
data class PlayBookRequest(
    val deviceInfo: DeviceInfoRequest,
    val forceDirectPlay: Boolean,
    val forceTranscode: Boolean,
    val supportedMimeTypes: List<String>,
    val mediaPlayer: String
)

@Serializable
data class PlayBookResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("userId")
    val userId: String = "",
    @SerialName("libraryId")
    val libraryId: String = "",
    @SerialName("libraryItemId")
    val libraryItemId: String = "",
    @SerialName("episodeId")
    val episodeId: String? = null,
    @SerialName("mediaType")
    val mediaType: String = "",
    @SerialName("mediaMetadata")
    val mediaMetadata: BookMetadata = BookMetadata(),
    @SerialName("chapters")
    val chapters: List<BookChapter> = emptyList(),
    @SerialName("displayTitle")
    val displayTitle: String = "",
    @SerialName("displayAuthor")
    val displayAuthor: String = "",
    @SerialName("coverPath")
    val coverPath: String = "",
    @SerialName("duration")
    val duration: Double = 0.0,
    @SerialName("playMethod")
    val playMethod: Int = 0,
    @SerialName("mediaPlayer")
    val mediaPlayer: String = "",
    @SerialName("deviceInfo")
    val deviceInfo: DeviceInfo = DeviceInfo(),
    @SerialName("serverVersion")
    val serverVersion: String = "",
    @SerialName("date")
    val date: String = "",
    @SerialName("dayOfWeek")
    val dayOfWeek: String = "",
    @SerialName("timeListening")
    val timeListening: Float = 0f,
    @SerialName("startTime")
    val startTime: Float = 0f,
    @SerialName("currentTime")
    val currentTime: Float = 0f,
    @SerialName("startedAt")
    val startedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0,
)

@Serializable
data class DeviceInfo(
    @SerialName("id")
    val id: String = "",
    @SerialName("userId")
    val userId: String = "",
    @SerialName("deviceId")
    val deviceId: String = "",
    @SerialName("ipAddress")
    val ipAddress: String? = null,
    @SerialName("browserName")
    val browserName: String? = null,
    @SerialName("browserVersion")
    val browserVersion: String? = null,
    @SerialName("osName")
    val osName: String? = null,
    @SerialName("osVersion")
    val osVersion: String? = null,
    @SerialName("deviceType")
    val deviceType: String? = null,
    @SerialName("manufacturer")
    val manufacturer: String? = null,
    @SerialName("model")
    val model: String? = null,
    @SerialName("sdkVersion")
    val sdkVersion: Int? = null,
    @SerialName("clientName")
    val clientName: String = "",
    @SerialName("clientVersion")
    val clientVersion: String = ""
)