package dev.halim.core.network.response

import dev.halim.core.network.response.libraryitem.LibraryItemSerializer
import dev.halim.core.network.response.libraryitem.Media
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryItemsResponse(
    @SerialName("results")
    val results: List<LibraryItem> = listOf(),
    @SerialName("total")
    val total: Int = 0,
    @SerialName("limit")
    val limit: Int = 0,
    @SerialName("page")
    val page: Int = 0,
    @SerialName("sortBy")
    val sortBy: String = "",
    @SerialName("sortDesc")
    val sortDesc: Boolean = false,
    @SerialName("filterBy")
    val filterBy: String = "",
    @SerialName("mediaType")
    val mediaType: String = "",
    @SerialName("minified")
    val minified: Boolean = false,
    @SerialName("collapseseries")
    val collapseseries: Boolean = false,
    @SerialName("include")
    val include: String = ""
)

@Serializable(with = LibraryItemSerializer::class)
data class LibraryItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("ino")
    val ino: String = "",
    @SerialName("libraryId")
    val libraryId: String = "",
    @SerialName("folderId")
    val folderId: String = "",
    @SerialName("path")
    val path: String = "",
    @SerialName("relPath")
    val relPath: String = "",
    @SerialName("isFile")
    val isFile: Boolean = false,
    @SerialName("mtimeMs")
    val mtimeMs: Long = 0,
    @SerialName("ctimeMs")
    val ctimeMs: Long = 0,
    @SerialName("birthtimeMs")
    val birthtimeMs: Long = 0,
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0,
    @SerialName("lastScan")
    val lastScan: Long? = 0,
    @SerialName("scanVersion")
    val scanVersion: String? = "",
    @SerialName("isMissing")
    val isMissing: Boolean = false,
    @SerialName("isInvalid")
    val isInvalid: Boolean = false,
    @SerialName("mediaType")
    val mediaType: String = "",
    @SerialName("media")
    val media: Media,
    @SerialName("libraryFiles")
    val libraryFiles: List<LibraryFile> = listOf(),
)

@Serializable
data class LibraryFile(
    @SerialName("ino")
    val ino: String = "",
    @SerialName("metadata")
    val metadata: Metadata = Metadata(),
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0,
    @SerialName("fileType")
    val fileType: String = ""
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