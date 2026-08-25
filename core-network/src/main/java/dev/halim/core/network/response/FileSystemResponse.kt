package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileSystemResponse(
  @SerialName("posix") val posix: Boolean = true,
  @SerialName("directories") val directories: List<FileSystemDirectory> = emptyList(),
)

@Serializable
data class FileSystemDirectory(
  @SerialName("path") val path: String = "",
  @SerialName("dirname") val dirname: String = "",
  @SerialName("level") val level: Int = 0,
)
