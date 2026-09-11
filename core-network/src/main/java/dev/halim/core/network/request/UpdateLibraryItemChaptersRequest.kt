package dev.halim.core.network.request

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLibraryItemChaptersRequest(
  @SerialName("chapters") val chapters: List<Chapter>,
) {
  @Serializable
  data class Chapter(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("id") val id: Int,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("start") val start: Double,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("end") val end: Double,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("title") val title: String,
  )
}
