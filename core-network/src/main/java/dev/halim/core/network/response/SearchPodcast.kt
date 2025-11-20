package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchPodcast(
  @SerialName("id") val id: Int,
  @SerialName("artistId") val artistId: Int?,
  @SerialName("title") val title: String,
  @SerialName("artistName") val artistName: String,
  @SerialName("description") val description: String,
  @SerialName("descriptionPlain") val descriptionPlain: String,
  @SerialName("releaseDate") val releaseDate: String,
  @SerialName("genres") val genres: List<String>,
  @SerialName("cover") val cover: String,
  @SerialName("trackCount") val trackCount: Int,
  @SerialName("feedUrl") val feedUrl: String,
  @SerialName("pageUrl") val pageUrl: String,
  @SerialName("explicit") val explicit: Boolean,
)
