package dev.halim.shelfdroid.network.libraryitem


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioTrack(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("startOffset")
    val startOffset: Int = 0,
    @SerialName("duration")
    val duration: Double = 0.0,
    @SerialName("title")
    val title: String = "",
    @SerialName("contentUrl")
    val contentUrl: String = "",
    @SerialName("mimeType")
    val mimeType: String = "",
    @SerialName("codec")
    val codec: String = "",
    @SerialName("metadata")
    val metadata: Metadata = Metadata()
)

@Serializable
data class Metadata(
    @SerialName("filename")
    val filename: String = "",
    @SerialName("ext")
    val ext: String = "",
    @SerialName("path")
    val path: String = "",
    @SerialName("relPath")
    val relPath: String = "",
    @SerialName("size")
    val size: Int = 0,
    @SerialName("mtimeMs")
    val mtimeMs: Long = 0,
    @SerialName("ctimeMs")
    val ctimeMs: Long = 0,
    @SerialName("birthtimeMs")
    val birthtimeMs: Long = 0
)