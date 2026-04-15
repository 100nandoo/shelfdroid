package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLibraryItemMediaRequest(
  @SerialName("metadata") val metadata: Metadata? = null,
  @SerialName("tags") val tags: List<String>? = null,
) {
  @Serializable
  data class Metadata(
    @SerialName("title") val title: String? = null,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("authors") val authors: List<NameRef>? = null,
    @SerialName("narrators") val narrators: List<String>? = null,
    @SerialName("series") val series: List<SeriesRef>? = null,
    @SerialName("genres") val genres: List<String>? = null,
    @SerialName("publishedYear") val publishedYear: String? = null,
    @SerialName("publisher") val publisher: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("isbn") val isbn: String? = null,
    @SerialName("asin") val asin: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("explicit") val explicit: Boolean? = null,
    @SerialName("abridged") val abridged: Boolean? = null,
  )

  @Serializable data class NameRef(@SerialName("name") val name: String)

  @Serializable
  data class SeriesRef(
    @SerialName("name") val name: String,
    @SerialName("sequence") val sequence: String? = null,
  )
}
