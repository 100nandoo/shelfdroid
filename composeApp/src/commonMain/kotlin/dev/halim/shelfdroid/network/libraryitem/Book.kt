package dev.halim.shelfdroid.network.libraryitem


import dev.halim.shelfdroid.network.FileMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
class Book(
    @SerialName("libraryItemId")
    override val libraryItemId: String = "",
    @SerialName("coverPath")
    override val coverPath: String? = null,
    @SerialName("tags")
    override val tags: List<String> = listOf(),
    @SerialName("metadata")
    val metadata: BookMetadata = BookMetadata(),
    @SerialName("audioFiles")
    val audioFiles: List<AudioFile> = listOf(),
    @SerialName("chapters")
    val chapters: List<BookChapter> = listOf(),
    @SerialName("ebookFile")
    val ebookFile: EbookFile? = EbookFile()
) : Media()

@Serializable
data class BookChapter(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("start")
    val start: Double = 0.0,
    @SerialName("end")
    val end: Double = 0.0,
    @SerialName("title")
    val title: String = ""
)

@Serializable
data class EbookFile(
    @SerialName("ino")
    val ino: String = "",
    @SerialName("metadata")
    val metadata: FileMetadata = FileMetadata(),
    @SerialName("ebookFormat")
    val ebookFormat: String = "",
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0
)