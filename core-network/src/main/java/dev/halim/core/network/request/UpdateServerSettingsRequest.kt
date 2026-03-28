package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateServerSettingsRequest(
  @SerialName("logLevel") val logLevel: Int? = null,
  @SerialName("backupPath") val backupPath: String? = null,
  @SerialName("backupSchedule") val backupSchedule: String? = null,
  @SerialName("backupsToKeep") val backupsToKeep: Int? = null,
  @SerialName("maxBackupSize") val maxBackupSize: Int? = null,
)
