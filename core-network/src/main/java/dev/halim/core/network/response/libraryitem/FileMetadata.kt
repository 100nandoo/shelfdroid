package dev.halim.core.network.response.libraryitem


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileMetadata(
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