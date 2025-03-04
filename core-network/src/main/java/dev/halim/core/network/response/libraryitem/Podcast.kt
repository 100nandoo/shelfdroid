package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Podcast(
    @SerialName("libraryItemId")
    override val libraryItemId: String = "",
    @SerialName("coverPath")
    override val coverPath: String?,
    @SerialName("tags")
    override val tags: List<String>,
    @SerialName("metadata")
    val metadata: PodcastMetadata = PodcastMetadata(),
    @SerialName("episodes")
    val episodes: List<PodcastEpisode> = listOf(),
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
data class PodcastMetadata(
    @SerialName("title")
    val title: String? = "",
    @SerialName("author")
    val author: String? = "",
    @SerialName("description")
    val description: String? = "",
    @SerialName("releaseDate")
    val releaseDate: String? = "",
    @SerialName("genres")
    val genres: List<String> = listOf(),
    @SerialName("feedUrl")
    val feedUrl: String? = "",
    @SerialName("imageUrl")
    val imageUrl: String? = "",
    @SerialName("itunesPageUrl")
    val itunesPageUrl: String? = "",
    @SerialName("itunesId")
    val itunesId: Int? = 0,
    @SerialName("itunesArtistId")
    val itunesArtistId: Int? = 0,
    @SerialName("explicit")
    val explicit: Boolean = false,
    @SerialName("language")
    val language: String? = "",
    @SerialName("type")
    val type: String? = ""
)

@Serializable
data class PodcastEpisode(
    @SerialName("libraryItemId")
    val libraryItemId: String = "",
    @SerialName("id")
    val id: String = "",
    @SerialName("index")
    val index: Int? = null,
    @SerialName("season")
    val season: String? = null,
    @SerialName("episode")
    val episode: String? = null,
    @SerialName("episodeType")
    val episodeType: String? = null,
    @SerialName("title")
    val title: String = "",
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("enclosure")
    val enclosure: Enclosure? = null,
    @SerialName("pubDate")
    val pubDate: String? = null,
    @SerialName("audioFile")
    val audioFile: AudioFile = AudioFile(),
    @SerialName("publishedAt")
    val publishedAt: Long? = null,
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0
)

@Serializable
data class Enclosure(
    @SerialName("url")
    val url: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("length")
    val length: String = ""
)