package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Media {
    abstract val libraryItemId: String
    abstract val coverPath: String?
    abstract val tags: List<String>
}

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
data class AudioMetaTags(
    @SerialName("tagAlbum")
    val tagAlbum: String = "",
    @SerialName("tagArtist")
    val tagArtist: String = "",
    @SerialName("tagGenre")
    val tagGenre: String = "",
    @SerialName("tagTitle")
    val tagTitle: String = "",
    @SerialName("tagSeries")
    val tagSeries: String? = "",
    @SerialName("tagSeriesPart")
    val tagSeriesPart: String? = "",
    @SerialName("tagTrack")
    val tagTrack: String = "",
    @SerialName("tagDisc")
    val tagDisc: String? = "",
    @SerialName("tagSubtitle")
    val tagSubtitle: String? = "",
    @SerialName("tagAlbumArtist")
    val tagAlbumArtist: String = "",
    @SerialName("tagDate")
    val tagDate: String? = "",
    @SerialName("tagComposer")
    val tagComposer: String = "",
    @SerialName("tagPublisher")
    val tagPublisher: String? = "",
    @SerialName("tagComment")
    val tagComment: String? = "",
    @SerialName("tagDescription")
    val tagDescription: String? = "",
    @SerialName("tagEncoder")
    val tagEncoder: String? = "",
    @SerialName("tagEncodedBy")
    val tagEncodedBy: String? = "",
    @SerialName("tagIsbn")
    val tagIsbn: String? = "",
    @SerialName("tagLanguage")
    val tagLanguage: String? = "",
    @SerialName("tagASIN")
    val tagASIN: String? = "",
    @SerialName("tagOverdriveMediaMarker")
    val tagOverdriveMediaMarker: String? = "",
    @SerialName("tagOriginalYear")
    val tagOriginalYear: String? = "",
    @SerialName("tagReleaseCountry")
    val tagReleaseCountry: String? = "",
    @SerialName("tagReleaseType")
    val tagReleaseType: String? = "",
    @SerialName("tagReleaseStatus")
    val tagReleaseStatus: String? = "",
    @SerialName("tagISRC")
    val tagISRC: String? = "",
    @SerialName("tagMusicBrainzTrackId")
    val tagMusicBrainzTrackId: String? = "",
    @SerialName("tagMusicBrainzAlbumId")
    val tagMusicBrainzAlbumId: String? = "",
    @SerialName("tagMusicBrainzAlbumArtistId")
    val tagMusicBrainzAlbumArtistId: String? = "",
    @SerialName("tagMusicBrainzArtistId")
    val tagMusicBrainzArtistId: String? = ""
)