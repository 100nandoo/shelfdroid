package dev.halim.shelfdroid.network.libraryitem


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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