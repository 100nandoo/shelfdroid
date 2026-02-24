package dev.halim.shelfdroid.core.navigation

import dev.halim.shelfdroid.core.UserType
import kotlinx.serialization.Serializable

@Serializable
data class PodcastFeedNavPayload(
  val id: String = "",
  val itunesId: Int = 0,
  val itunesArtistId: Int? = null,
  val libraryId: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val genre: String = "",
  val episodeCount: Int = 0,
  val feedUrl: String = "",
  val pageUrl: String = "",
  val releaseDate: String = "",
  val explicit: Boolean = false,
  val isAdded: Boolean = false,
)

@Serializable
data class NavEditUser(
  val id: String = "",
  val username: String = "",
  val password: String = "",
  val email: String = "",
  val type: UserType = UserType.Unknown,
  val isActive: Boolean = false,
  val librariesAccessible: List<String> = listOf(),
  val itemTagsAccessible: List<String> = listOf(),
  val invert: Boolean = false,
  val permissions: String = "",
) {
  fun isCreateMode() = username.isEmpty() && password.isEmpty()

  companion object {
    fun defaultUser() = NavEditUser(type = UserType.User, isActive = true)
  }
}
