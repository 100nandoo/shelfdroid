package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

@Serializable
data class CrudPrefs(
  // Library item
  val hardDelete: Boolean = true,
  // Episode
  // * delete
  val episodeHardDelete: Boolean = true,
  val episodeAutoSelectFinished: Boolean = true,
  // * add
  val addEpisodeHideDownloaded: Boolean = true,
)
