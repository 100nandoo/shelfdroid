package dev.halim.shelfdroid.network.login


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerSettings(
    @SerialName("authActiveAuthMethods")
    val authActiveAuthMethods: List<String> = listOf(),
    @SerialName("authOpenIDAutoLaunch")
    val authOpenIDAutoLaunch: Boolean = false,
    @SerialName("authOpenIDAutoRegister")
    val authOpenIDAutoRegister: Boolean = false,
    @SerialName("authOpenIDButtonText")
    val authOpenIDButtonText: String = "",
    @SerialName("authOpenIDTokenSigningAlgorithm")
    val authOpenIDTokenSigningAlgorithm: String = "",
    @SerialName("backupPath")
    val backupPath: String = "",
    @SerialName("backupSchedule")
    val backupSchedule: Boolean = false,
    @SerialName("backupsToKeep")
    val backupsToKeep: Int = 0,
    @SerialName("bookshelfView")
    val bookshelfView: Int = 0,
    @SerialName("buildNumber")
    val buildNumber: Int = 0,
    @SerialName("chromecastEnabled")
    val chromecastEnabled: Boolean = false,
    @SerialName("dateFormat")
    val dateFormat: String = "",
    @SerialName("homeBookshelfView")
    val homeBookshelfView: Int = 0,
    @SerialName("id")
    val id: String = "",
    @SerialName("language")
    val language: String = "",
    @SerialName("logLevel")
    val logLevel: Int = 0,
    @SerialName("loggerDailyLogsToKeep")
    val loggerDailyLogsToKeep: Int = 0,
    @SerialName("loggerScannerLogsToKeep")
    val loggerScannerLogsToKeep: Int = 0,
    @SerialName("maxBackupSize")
    val maxBackupSize: Int = 0,
    @SerialName("metadataFileFormat")
    val metadataFileFormat: String = "",
    @SerialName("podcastEpisodeSchedule")
    val podcastEpisodeSchedule: String = "",
    @SerialName("rateLimitLoginRequests")
    val rateLimitLoginRequests: Int = 0,
    @SerialName("rateLimitLoginWindow")
    val rateLimitLoginWindow: Int = 0,
    @SerialName("scannerCoverProvider")
    val scannerCoverProvider: String = "",
    @SerialName("scannerDisableWatcher")
    val scannerDisableWatcher: Boolean = false,
    @SerialName("scannerFindCovers")
    val scannerFindCovers: Boolean = false,
    @SerialName("scannerParseSubtitle")
    val scannerParseSubtitle: Boolean = false,
    @SerialName("scannerPreferMatchedMetadata")
    val scannerPreferMatchedMetadata: Boolean = false,
    @SerialName("sortingIgnorePrefix")
    val sortingIgnorePrefix: Boolean = false,
    @SerialName("sortingPrefixes")
    val sortingPrefixes: List<String> = listOf(),
    @SerialName("storeCoverWithItem")
    val storeCoverWithItem: Boolean = false,
    @SerialName("storeMetadataWithItem")
    val storeMetadataWithItem: Boolean = false,
    @SerialName("timeFormat")
    val timeFormat: String = "",
    @SerialName("version")
    val version: String = ""
)