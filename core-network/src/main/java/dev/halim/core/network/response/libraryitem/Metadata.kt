package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Metadata {
  abstract val title: String?
  abstract val language: String?
  abstract val explicit: Boolean
  abstract val description: String?
  abstract val genres: List<String>
}

@Serializable
data class BookMetadata(
  @SerialName("title") override val title: String? = "",
  @SerialName("language") override val language: String? = "",
  @SerialName("explicit") override val explicit: Boolean = false,
  @SerialName("description") override val description: String? = "",
  @SerialName("genres") override val genres: List<String> = listOf(),
  @SerialName("subtitle") val subtitle: String? = "",
  @SerialName("authors") val authors: List<Author> = listOf(),
  @SerialName("narrators") val narrators: List<String> = listOf(),
  @SerialName("series") val series: List<Series> = listOf(),
  @SerialName("publishedYear") val publishedYear: String? = "",
  @SerialName("publishedDate") val publishedDate: String? = "",
  @SerialName("publisher") val publisher: String? = "",
  @SerialName("isbn") val isbn: String? = "",
  @SerialName("asin") val asin: String? = "",
  @SerialName("descriptionPlain") val descriptionPlain: String? = "",
) : Metadata()

@Serializable
data class PodcastMetadata(
  @SerialName("title") override val title: String? = "",
  @SerialName("language") override val language: String? = "",
  @SerialName("explicit") override val explicit: Boolean = false,
  @SerialName("description") override val description: String? = "",
  @SerialName("genres") override val genres: List<String> = listOf(),
  @SerialName("author") val author: String? = "",
  @SerialName("releaseDate") val releaseDate: String? = "",
  @SerialName("feedUrl") val feedUrl: String? = "",
  @SerialName("imageUrl") val imageUrl: String? = "",
  @SerialName("itunesPageUrl") val itunesPageUrl: String? = "",
  @SerialName("itunesId") val itunesId: Int? = 0,
  @SerialName("itunesArtistId") val itunesArtistId: Int? = 0,
  @SerialName("type") val type: String? = "",
) : Metadata()

@Serializable
data class Author(
  @SerialName("id") val id: String = "",
  @SerialName("asin") val asin: String? = "",
  @SerialName("name") val name: String = "",
  @SerialName("description") val description: String? = "",
  @SerialName("imagePath") val imagePath: String? = "",
  @SerialName("addedAt") val addedAt: Long = 0,
  @SerialName("updatedAt") val updatedAt: Long = 0,
)

@Serializable
data class Series(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("description") val description: String? = "",
  @SerialName("addedAt") val addedAt: Long = 0,
  @SerialName("updatedAt") val updatedAt: Long = 0,
)
