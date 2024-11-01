package dev.halim.shelfdroid.network.libraryitem


import dev.halim.shelfdroid.network.FileMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class Media {
    abstract val libraryItemId: String
    abstract val coverPath: String?
    abstract val tags: List<String>
}

@Serializable
class Book(
    @SerialName("libraryItemId")
    override val libraryItemId: String = "",
    @SerialName("coverPath")
    override val coverPath: String?,
    @SerialName("tags")
    override val tags: List<String>,
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
class Podcast(
    @SerialName("libraryItemId")
    override val libraryItemId: String = "",
    @SerialName("coverPath")
    override val coverPath: String?,
    @SerialName("tags")
    override val tags: List<String>,
    @SerialName("autoDownloadEpisodes")
    val autoDownloadEpisodes: Boolean = false,
    @SerialName("autoDownloadSchedule")
    val autoDownloadSchedule: String = "",
    @SerialName("lastEpisodeCheck")
    val lastEpisodeCheck: Long = 0,
    @SerialName("maxEpisodesToKeep")
    val maxEpisodesToKeep: Int = 0,
    @SerialName("maxNewEpisodesToDownload")
    val maxNewEpisodesToDownload: Int = 0
) : Media()

@Serializable
data class AudioFile(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("ino")
    val ino: String = "",
    @SerialName("metadata")
    val metadata: FileMetadata = FileMetadata(),
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0,
    @SerialName("trackNumFromMeta")
    val trackNumFromMeta: Int? = 0,
    @SerialName("discNumFromMeta")
    val discNumFromMeta: Int? = 0,
    @SerialName("trackNumFromFilename")
    val trackNumFromFilename: Int? = 0,
    @SerialName("discNumFromFilename")
    val discNumFromFilename: Int? = 0,
    @SerialName("manuallyVerified")
    val manuallyVerified: Boolean = false,
    @SerialName("exclude")
    val exclude: Boolean = false,
    @SerialName("error")
    val error: String? = "",
    @SerialName("format")
    val format: String = "",
    @SerialName("duration")
    val duration: Double = 0.0,
    @SerialName("bitRate")
    val bitRate: Int = 0,
    @SerialName("language")
    val language: String? = "",
    @SerialName("codec")
    val codec: String = "",
    @SerialName("timeBase")
    val timeBase: String = "",
    @SerialName("channels")
    val channels: Int = 0,
    @SerialName("channelLayout")
    val channelLayout: String = "",
    @SerialName("chapters")
    val chapters: List<BookChapter> = listOf(),
    @SerialName("embeddedCoverArt")
    val embeddedCoverArt: String? = "",
    @SerialName("metaTags")
    val metaTags: AudioMetaTags = AudioMetaTags(),
    @SerialName("mimeType")
    val mimeType: String = ""
)

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