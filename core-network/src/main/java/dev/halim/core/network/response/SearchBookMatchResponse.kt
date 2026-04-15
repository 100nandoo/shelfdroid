package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchBookMatchResponse(
  @SerialName("title") val title: String? = null,
  @SerialName("subtitle") val subtitle: String? = null,
  @SerialName("author") val author: String? = null,
  @SerialName("narrator") val narrator: String? = null,
  @SerialName("publisher") val publisher: String? = null,
  @SerialName("publishedYear") val publishedYear: String? = null,
  @SerialName("description") val description: String? = null,
  @SerialName("cover") val cover: String? = null,
  @SerialName("isbn") val isbn: String? = null,
  @SerialName("asin") val asin: String? = null,
  @SerialName("genres") val genres: List<String> = emptyList(),
  @SerialName("tags") val tags: List<String> = emptyList(),
  @SerialName("series") val series: List<SeriesRef> = emptyList(),
) {
  @Serializable
  data class SeriesRef(
    @SerialName("series") val series: String? = null,
    @SerialName("sequence") val sequence: String? = null,
  )
}
