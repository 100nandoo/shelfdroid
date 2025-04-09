package dev.halim.core.network.response.libraryitem


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BookMetadata(
    @SerialName("title")
    val title: String? = "",
    @SerialName("subtitle")
    val subtitle: String? = "",
    @SerialName("authors")
    val authors: List<Author> = listOf(),
    @SerialName("narrators")
    val narrators: List<String> = listOf(),
    @SerialName("series")
    val series: List<Series> = listOf(),
    @SerialName("genres")
    val genres: List<String> = listOf(),
    @SerialName("publishedYear")
    val publishedYear: String? = "",
    @SerialName("publishedDate")
    val publishedDate: String? = "",
    @SerialName("publisher")
    val publisher: String? = "",
    @SerialName("description")
    val description: String? = "",
    @SerialName("isbn")
    val isbn: String? = "",
    @SerialName("asin")
    val asin: String? = "",
    @SerialName("language")
    val language: String? = "",
    @SerialName("explicit")
    val explicit: Boolean = false,
    @SerialName("descriptionPlain")
    val descriptionPlain: String? = "",
)

@Serializable
data class Author(
    @SerialName("id")
    val id: String = "",
    @SerialName("asin")
    val asin: String? = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String? = "",
    @SerialName("imagePath")
    val imagePath: String? = "",
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0
)

@Serializable
data class Series(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String? = "",
    @SerialName("addedAt")
    val addedAt: Long = 0,
    @SerialName("updatedAt")
    val updatedAt: Long = 0
)