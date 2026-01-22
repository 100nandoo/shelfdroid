package dev.halim.shelfdroid.core.ui.screen.addepisode

import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import dev.halim.shelfdroid.core.data.screen.addepisode.TextFilter

fun filter(episodes: List<AddEpisode>, filterState: AddEpisodeFilterState): List<AddEpisode> {
  val query = filterState.text

  return episodes.filter { episode ->
    val passesDownloaded =
      !filterState.hideDownloaded || episode.state != AddEpisodeDownloadState.Downloaded

    val passesText =
      when (filterState.textFilter) {
        TextFilter.TITLE -> episode.title.contains(query, ignoreCase = true)

        TextFilter.DESCRIPTION -> episode.description.contains(query, ignoreCase = true)

        TextFilter.BOTH ->
          episode.title.contains(query, ignoreCase = true) ||
            episode.description.contains(query, ignoreCase = true)
      }

    passesDownloaded && passesText
  }
}
