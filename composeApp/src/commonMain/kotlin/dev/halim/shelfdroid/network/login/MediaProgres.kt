package dev.halim.shelfdroid.network.login


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaProgres(
    @SerialName("currentTime")
    val currentTime: Double = 0.0,
    @SerialName("duration")
    val duration: Double = 0.0,
    @SerialName("ebookProgress")
    val ebookProgress: Int? = 0,
    @SerialName("episodeId")
    val episodeId: String? = "",
    @SerialName("finishedAt")
    val finishedAt: Long? = 0,
    @SerialName("hideFromContinueListening")
    val hideFromContinueListening: Boolean = false,
    @SerialName("id")
    val id: String = "",
    @SerialName("isFinished")
    val isFinished: Boolean = false,
    @SerialName("lastUpdate")
    val lastUpdate: Long = 0,
    @SerialName("libraryItemId")
    val libraryItemId: String = "",
    @SerialName("mediaItemId")
    val mediaItemId: String = "",
    @SerialName("mediaItemType")
    val mediaItemType: String = "",
    @SerialName("progress")
    val progress: Double = 0.0,
    @SerialName("startedAt")
    val startedAt: Long = 0,
    @SerialName("userId")
    val userId: String = ""
)