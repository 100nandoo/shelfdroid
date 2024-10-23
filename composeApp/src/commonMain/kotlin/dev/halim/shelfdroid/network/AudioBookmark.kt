package dev.halim.shelfdroid.network


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioBookmark(
    @SerialName("createdAt")
    val createdAt: Long = 0,
    @SerialName("libraryItemId")
    val libraryItemId: String = "",
    @SerialName("time")
    val time: Int = 0,
    @SerialName("title")
    val title: String = ""
)