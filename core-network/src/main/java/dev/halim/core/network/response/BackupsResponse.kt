package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupsResponse(
  @SerialName("backups") val backups: List<Backup>,
  @SerialName("backupLocation") val backupLocation: String? = null,
) {
  @Serializable
  data class Backup(
    @SerialName("id") val id: String,
    @SerialName("backupDirPath") val backupDirPath: String,
    @SerialName("fullPath") val fullPath: String,
    @SerialName("path") val path: String,
    @SerialName("filename") val filename: String,
    @SerialName("fileSize") val fileSize: Long,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("serverVersion") val serverVersion: String,
  )
}
