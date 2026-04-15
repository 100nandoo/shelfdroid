package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrarySeriesResponse(
  @SerialName("results") val results: List<LibrarySeries> = listOf(),
  @SerialName("total") val total: Int = 0,
  @SerialName("limit") val limit: Int = 0,
  @SerialName("page") val page: Int = 0,
  @SerialName("sortBy") val sortBy: String = "",
  @SerialName("sortDesc") val sortDesc: Boolean = false,
  @SerialName("filterBy") val filterBy: String = "",
  @SerialName("minified") val minified: Boolean = false,
  @SerialName("include") val include: String = "",
)

@Serializable
data class LibrarySeries(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("nameIgnorePrefix") val nameIgnorePrefix: String = "",
  @SerialName("nameIgnorePrefixSort") val nameIgnorePrefixSort: String = "",
  @SerialName("type") val type: String = "series",
  @SerialName("books") val books: List<LibraryItem> = listOf(),
  @SerialName("addedAt") val addedAt: Long = 0,
  @SerialName("totalDuration") val totalDuration: Double = 0.0,
)
