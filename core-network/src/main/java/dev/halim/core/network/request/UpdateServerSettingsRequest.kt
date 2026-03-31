package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateServerSettingsRequest(
  // General
  @SerialName("storeCoverWithItem") val storeCoverWithItem: Boolean? = null,
  @SerialName("storeMetadataWithItem") val storeMetadataWithItem: Boolean? = null,
  @SerialName("sortingIgnorePrefix") val sortingIgnorePrefix: Boolean? = null,
  // Scanner
  @SerialName("scannerParseSubtitle") val scannerParseSubtitle: Boolean? = null,
  @SerialName("scannerFindCovers") val scannerFindCovers: Boolean? = null,
  @SerialName("scannerCoverProvider") val scannerCoverProvider: String? = null,
  @SerialName("scannerPreferMatchedMetadata") val scannerPreferMatchedMetadata: Boolean? = null,
  @SerialName("scannerDisableWatcher") val scannerDisableWatcher: Boolean? = null,
  // Web Client
  @SerialName("chromecastEnabled") val chromecastEnabled: Boolean? = null,
  @SerialName("allowIframe") val allowIframe: Boolean? = null,
  // Display
  @SerialName("homeBookshelfView") val homeBookshelfView: Int? = null,
  @SerialName("bookshelfView") val bookshelfView: Int? = null,
  @SerialName("dateFormat") val dateFormat: String? = null,
  @SerialName("timeFormat") val timeFormat: String? = null,
  @SerialName("language") val language: String? = null,
  // Backup
  @SerialName("backupPath") val backupPath: String? = null,
  @SerialName("backupSchedule") val backupSchedule: String? = null,
  @SerialName("backupsToKeep") val backupsToKeep: Int? = null,
  @SerialName("maxBackupSize") val maxBackupSize: Int? = null,
  // Logger
  @SerialName("logLevel") val logLevel: Int? = null,
)
