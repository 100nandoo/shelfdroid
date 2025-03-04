package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrariesResponse(
    @SerialName("libraries")
    val libraries: List<Library> = listOf(),
)

@Serializable
data class Library(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("folders")
    val folders: List<Folder> = listOf(),
    @SerialName("displayOrder")
    val displayOrder: Int = 0,
    @SerialName("icon")
    val icon: String = "",
    @SerialName("mediaType")
    val mediaType: String = "",
    @SerialName("provider")
    val provider: String = "",
    @SerialName("settings")
    val settings: LibrarySettings = LibrarySettings(),
    @SerialName("createdAt")
    val createdAt: Long = 0,
    @SerialName("lastUpdate")
    val lastUpdate: Long = 0
)

@Serializable
data class Folder(
    @SerialName("id")
    val id: String = "",
    @SerialName("fullPath")
    val fullPath: String = "",
    @SerialName("libraryId")
    val libraryId: String = "",
    @SerialName("addedAt")
    val addedAt: Long = 0
)

@Serializable
data class LibrarySettings(
    @SerialName("coverAspectRatio")
    val coverAspectRatio: Int = 0,
    @SerialName("disableWatcher")
    val disableWatcher: Boolean = false,
    @SerialName("skipMatchingMediaWithAsin")
    val skipMatchingMediaWithAsin: Boolean = false,
    @SerialName("skipMatchingMediaWithIsbn")
    val skipMatchingMediaWithIsbn: Boolean = false,
    @SerialName("autoScanCronExpression")
    val autoScanCronExpression: String? = ""
)