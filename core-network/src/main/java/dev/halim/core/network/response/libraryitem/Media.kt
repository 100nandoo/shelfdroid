package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface Media {
  val libraryItemId: String
  val coverPath: String?
  val tags: List<String>
}

@Serializable
data class AudioFile(
  @SerialName("index") val index: Int = 0,
  @SerialName("ino") val ino: String = "",
  @SerialName("metadata") val metadata: FileMetadata = FileMetadata(),
  @SerialName("addedAt") val addedAt: Long = 0,
  @SerialName("updatedAt") val updatedAt: Long = 0,
  @SerialName("trackNumFromMeta") val trackNumFromMeta: Int? = null,
  @SerialName("discNumFromMeta") val discNumFromMeta: Int? = null,
  @SerialName("trackNumFromFilename") val trackNumFromFilename: Int? = null,
  @SerialName("discNumFromFilename") val discNumFromFilename: Int? = null,
  @SerialName("manuallyVerified") val manuallyVerified: Boolean = false,
  @SerialName("exclude") val exclude: Boolean = false,
  @SerialName("error") val error: String? = null,
  @SerialName("format") val format: String = "",
  @SerialName("duration") val duration: Double = 0.0,
  @SerialName("bitRate") val bitRate: Int = 0,
  @SerialName("language") val language: String? = null,
  @SerialName("codec") val codec: String = "",
  @SerialName("timeBase") val timeBase: String = "",
  @SerialName("channels") val channels: Int = 0,
  @SerialName("channelLayout") val channelLayout: String = "",
  @SerialName("chapters") val chapters: List<BookChapter> = listOf(),
  @SerialName("embeddedCoverArt") val embeddedCoverArt: String? = null,
  @SerialName("metaTags") val metaTags: AudioMetaTags = AudioMetaTags(),
  @SerialName("mimeType") val mimeType: String = "",
)

@Serializable
data class AudioMetaTags(
  @SerialName("tagAlbum") val tagAlbum: String = "",
  @SerialName("tagArtist") val tagArtist: String = "",
  @SerialName("tagGenre") val tagGenre: String = "",
  @SerialName("tagTitle") val tagTitle: String = "",
  @SerialName("tagSeries") val tagSeries: String? = null,
  @SerialName("tagSeriesPart") val tagSeriesPart: String? = null,
  @SerialName("tagTrack") val tagTrack: String = "",
  @SerialName("tagDisc") val tagDisc: String? = null,
  @SerialName("tagSubtitle") val tagSubtitle: String? = null,
  @SerialName("tagAlbumArtist") val tagAlbumArtist: String = "",
  @SerialName("tagDate") val tagDate: String? = null,
  @SerialName("tagComposer") val tagComposer: String = "",
  @SerialName("tagPublisher") val tagPublisher: String? = null,
  @SerialName("tagComment") val tagComment: String? = null,
  @SerialName("tagDescription") val tagDescription: String? = null,
  @SerialName("tagEncoder") val tagEncoder: String? = null,
  @SerialName("tagEncodedBy") val tagEncodedBy: String? = null,
  @SerialName("tagIsbn") val tagIsbn: String? = null,
  @SerialName("tagLanguage") val tagLanguage: String? = null,
  @SerialName("tagASIN") val tagASIN: String? = null,
  @SerialName("tagOverdriveMediaMarker") val tagOverdriveMediaMarker: String? = null,
  @SerialName("tagOriginalYear") val tagOriginalYear: String? = null,
  @SerialName("tagReleaseCountry") val tagReleaseCountry: String? = null,
  @SerialName("tagReleaseType") val tagReleaseType: String? = null,
  @SerialName("tagReleaseStatus") val tagReleaseStatus: String? = null,
  @SerialName("tagISRC") val tagISRC: String? = null,
  @SerialName("tagMusicBrainzTrackId") val tagMusicBrainzTrackId: String? = null,
  @SerialName("tagMusicBrainzAlbumId") val tagMusicBrainzAlbumId: String? = null,
  @SerialName("tagMusicBrainzAlbumArtistId") val tagMusicBrainzAlbumArtistId: String? = null,
  @SerialName("tagMusicBrainzArtistId") val tagMusicBrainzArtistId: String? = null,
)
