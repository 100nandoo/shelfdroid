package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateLibraryRequest(
  @SerialName("name") val name: String,
  @SerialName("folders") val folders: List<Folder>,
  @SerialName("mediaType") val mediaType: String,
  @SerialName("icon") val icon: String,
  @SerialName("provider") val provider: String,
  @SerialName("settings") val settings: Settings? = null,
) {
  @Serializable data class Folder(@SerialName("path") val path: String)

  @Serializable
  data class Settings(
    @SerialName("coverAspectRatio") val coverAspectRatio: Int? = null,
    @SerialName("disableWatcher") val disableWatcher: Boolean? = null,
    @SerialName("audiobooksOnly") val audiobooksOnly: Boolean? = null,
    @SerialName("skipMatchingMediaWithAsin") val skipMatchingMediaWithAsin: Boolean? = null,
    @SerialName("skipMatchingMediaWithIsbn") val skipMatchingMediaWithIsbn: Boolean? = null,
    @SerialName("epubsAllowScriptedContent") val epubsAllowScriptedContent: Boolean? = null,
    @SerialName("hideSingleBookSeries") val hideSingleBookSeries: Boolean? = null,
    @SerialName("onlyShowLaterBooksInContinueSeries")
    val onlyShowLaterBooksInContinueSeries: Boolean? = null,
    @SerialName("podcastSearchRegion") val podcastSearchRegion: String? = null,
    @SerialName("metadataPrecedence") val metadataPrecedence: List<String>? = null,
    @SerialName("markAsFinishedPercentComplete") val markAsFinishedPercentComplete: Int? = null,
    @SerialName("markAsFinishedTimeRemaining") val markAsFinishedTimeRemaining: Int? = null,
    @SerialName("autoScanCronExpression") val autoScanCronExpression: String? = null,
  )
}
